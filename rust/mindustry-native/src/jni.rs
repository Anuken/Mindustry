//! JNI entry point for `mindustry.ai.RustPathfinder.build`.
//!
//! All Java arrays are copied into owned buffers, the field is computed there, and the
//! results are written back only on success. A failure leaves the Java arrays untouched,
//! so the caller can transparently fall back to the Java implementation.

#![allow(non_snake_case)]

use jni::objects::{JClass, JIntArray, JShortArray};
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

use crate::{build, BuildParams};

#[no_mangle]
pub extern "system" fn Java_mindustry_ai_RustPathfinder_build<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    tiles: JIntArray<'local>,
    weights: JIntArray<'local>,
    searches: JShortArray<'local>,
    targets: JIntArray<'local>,
    team: jint,
    cost_type: jint,
    search: jint,
    width: jint,
    height: jint,
) -> jboolean {
    let result = try_build(
        &mut env, &tiles, &weights, &searches, &targets, team, cost_type, search, width, height,
    );

    match result {
        Ok(true) => JNI_TRUE,
        _ => {
            // Never let a pending Java exception escape an FFI boundary.
            if env.exception_check().unwrap_or(false) {
                let _ = env.exception_clear();
            }
            JNI_FALSE
        }
    }
}

#[allow(clippy::too_many_arguments)]
fn try_build<'local>(
    env: &mut JNIEnv<'local>,
    tiles: &JIntArray<'local>,
    weights: &JIntArray<'local>,
    searches: &JShortArray<'local>,
    targets: &JIntArray<'local>,
    team: jint,
    cost_type: jint,
    search: jint,
    width: jint,
    height: jint,
) -> jni::errors::Result<bool> {
    if width <= 0 || height <= 0 {
        return Ok(false);
    }
    let (width, height) = (width as usize, height as usize);
    let len = match width.checked_mul(height) {
        Some(value) => value,
        None => return Ok(false),
    };

    let tiles_len = env.get_array_length(tiles)? as usize;
    let weights_len = env.get_array_length(weights)? as usize;
    let searches_len = env.get_array_length(searches)? as usize;
    let targets_len = env.get_array_length(targets)? as usize;

    if tiles_len < len || weights_len < len || searches_len < len {
        return Ok(false);
    }

    let mut tiles_buf = vec![0i32; tiles_len];
    env.get_int_array_region(tiles, 0, &mut tiles_buf)?;
    let mut targets_buf = vec![0i32; targets_len];
    env.get_int_array_region(targets, 0, &mut targets_buf)?;
    let mut weights_buf = vec![0i32; weights_len];
    env.get_int_array_region(weights, 0, &mut weights_buf)?;
    let mut searches_buf = vec![0i16; searches_len];
    env.get_short_array_region(searches, 0, &mut searches_buf)?;

    let ok = build(BuildParams {
        tiles: &tiles_buf,
        weights: &mut weights_buf,
        searches: &mut searches_buf,
        targets: &targets_buf,
        team,
        cost_type,
        search,
        width,
        height,
    });
    if !ok {
        return Ok(false);
    }

    env.set_int_array_region(weights, 0, &weights_buf)?;
    env.set_short_array_region(searches, 0, &searches_buf)?;
    Ok(true)
}
