//! Rust port of `mindustry.async.AvoidanceProcess.process()`.
//!
//! The Java version stamps a disk-shaped mask for every AI ground unit into a double-buffered
//! `int[]` avoidance grid on an async thread. This port does the same in one pass over owned
//! copies of the request and buffer arrays, so one JNI call covers the whole frame.

// Guard against pathological radii while still matching Java's (uncapped) r for sane values.
const MAX_RADIUS_TILES: i32 = 256;

pub struct AvoidanceParams<'a> {
    /// Triples of `(packed point, raw radius bits, unit id)`; length must be a multiple of 3.
    pub requests: &'a [i32],
    /// Double-buffered avoidance grid, row-major, at least `width * height` elements.
    pub buffer: &'a mut [i32],
    pub width: i32,
    pub height: i32,
    /// Java clears the target buffer when the previous frame had requests.
    pub clear: bool,
}

/// Fills the avoidance mask. Returns `false` for malformed input so Java can fall back.
pub fn build(p: AvoidanceParams<'_>) -> bool {
    if p.width <= 0 || p.height <= 0 || p.requests.len() % 3 != 0 {
        return false;
    }
    let len = match (p.width as usize).checked_mul(p.height as usize) {
        Some(value) => value,
        None => return false,
    };
    if len > i32::MAX as usize || p.buffer.len() < len {
        return false;
    }

    if p.clear {
        p.buffer.fill(0);
    }

    for chunk in p.requests.chunks_exact(3) {
        // Mirrors Arc's Point2.pack/x/y: two signed shorts packed into one int.
        let point = chunk[0] as u32;
        let rx = ((point >> 16) as u16 as i16) as i32;
        let ry = (point as u16 as i16) as i32;
        let id = chunk[2];
        let rad = f32::from_bits(chunk[1] as u32);
        if !rad.is_finite() {
            return false;
        }
        let rad2 = rad * rad;

        // Java calls Mathf.clamp(1, Mathf.ceil(rad), 20), which is max(1, ceil(rad)):
        // min(1, 20) == 1, so there is intentionally no upper cap here.
        let r = 1.max(mathf_ceil(rad));
        if r > MAX_RADIUS_TILES {
            return false;
        }

        for dx in -r..=r {
            for dy in -r..=r {
                // Java int arithmetic wraps; keep the same promotion to float for rad2.
                let dist2 = dx.wrapping_mul(dx).wrapping_add(dy.wrapping_mul(dy));
                let x = dx.wrapping_add(rx);
                let y = dy.wrapping_add(ry);
                if x >= 0 && y >= 0 && x < p.width && y < p.height && (dist2 as f32) <= rad2 {
                    let idx = x.wrapping_add(y.wrapping_mul(p.width));
                    if idx < 0 || idx as usize >= p.buffer.len() {
                        return false;
                    }
                    let value = i32::MAX.wrapping_sub(id);
                    let slot = &mut p.buffer[idx as usize];
                    *slot = (*slot).max(value);
                }
            }
        }
    }

    true
}

/// Arc's `Mathf.ceil(float)` uses a 16384 offset with double arithmetic.
fn mathf_ceil(value: f32) -> i32 {
    let truncated = (16384.0f64 - (value as f64)) as i32;
    16384i32.wrapping_sub(truncated)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn pack_point(x: i16, y: i16) -> i32 {
        ((x as u16 as i32) << 16) | ((y as u16 as i32) & 0xffff)
    }

    fn request(x: i16, y: i16, rad: f32, id: i32) -> [i32; 3] {
        [pack_point(x, y), rad.to_bits() as i32, id]
    }

    fn run(
        requests: &[i32],
        width: i32,
        height: i32,
        clear: bool,
        initial: &[i32],
    ) -> Option<Vec<i32>> {
        let mut buffer = initial.to_vec();
        let ok = build(AvoidanceParams {
            requests,
            buffer: &mut buffer,
            width,
            height,
            clear,
        });
        if !ok {
            return None;
        }
        Some(buffer)
    }

    #[test]
    fn small_radius_writes_cross() {
        let req = request(5, 5, 1.0, 7);
        let buffer = run(&req, 10, 10, false, &[0i32; 100]).expect("build should succeed");
        let mut expected = vec![0i32; 100];
        for (dx, dy) in [(0i32, 0i32), (-1, 0), (1, 0), (0, -1), (0, 1)] {
            let x = 5 + dx;
            let y = 5 + dy;
            expected[(x + y * 10) as usize] = i32::MAX - 7;
        }
        assert_eq!(buffer, expected);
    }

    #[test]
    fn clear_resets_buffer() {
        let req = request(2, 2, 0.0, 3);
        let buffer = run(&req, 5, 5, true, &[123i32; 25]).expect("build should succeed");
        let mut expected = vec![0i32; 25];
        expected[2 + 2 * 5] = i32::MAX - 3;
        assert_eq!(buffer, expected);
    }

    #[test]
    fn radius_is_not_capped_at_twenty() {
        let req = request(50, 50, 25.0, 1);
        let buffer = run(&req, 100, 100, false, &[0i32; 10_000]).expect("build should succeed");
        // dx = 21 is inside rad2 = 625, but would be missed if r were capped at 20.
        assert_eq!(buffer[71 + 50 * 100], i32::MAX - 1);
    }

    #[test]
    fn negative_coordinates() {
        let req = request(-1, 2, 1.0, 5);
        let buffer = run(&req, 5, 5, false, &[0i32; 25]).expect("build should succeed");
        let mut expected = vec![0i32; 25];
        expected[0 + 2 * 5] = i32::MAX - 5;
        assert_eq!(buffer, expected);
    }

    #[test]
    fn invalid_inputs() {
        let req = request(1, 1, 1.0, 1);
        let mut buffer = vec![0i32; 9];
        assert!(!build(AvoidanceParams {
            requests: &req,
            buffer: &mut buffer,
            width: 0,
            height: 3,
            clear: false,
        }));
        assert!(!build(AvoidanceParams {
            requests: &req[..2],
            buffer: &mut buffer,
            width: 3,
            height: 3,
            clear: false,
        }));
        let mut short = vec![0i32; 5];
        assert!(!build(AvoidanceParams {
            requests: &req,
            buffer: &mut short,
            width: 3,
            height: 3,
            clear: false,
        }));
    }

    #[test]
    fn non_finite_radius_is_rejected() {
        let req = [pack_point(2, 2), f32::NAN.to_bits() as i32, 1];
        let mut buffer = vec![0i32; 25];
        assert!(!build(AvoidanceParams {
            requests: &req,
            buffer: &mut buffer,
            width: 5,
            height: 5,
            clear: false,
        }));
    }

    #[test]
    fn mathf_ceil_matches_arc() {
        assert_eq!(mathf_ceil(2.0), 2);
        assert_eq!(mathf_ceil(2.5), 3);
    }
}
