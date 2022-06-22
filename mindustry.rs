use std::process::Command;

fn main() {
	Command::new("java").arg("-jar").arg("Mindustry.jar").spawn().expect("sus");
}
