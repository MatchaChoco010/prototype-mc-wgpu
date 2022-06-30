plugins {
    id("fr.stardustenterprises.rust.wrapper") version libs.versions.rust.importer
}

rust {
    release.set(true)
    command.set("cross")
    cargoInstallTargets.set(true)

    targets {
        create("win") {
            target = "x86_64-pc-windows-msvc"
            outputName = "wgpu_renderer.dll"
        }
        create("linux-aarch64") {
            target = "aarch64-unknown-linux-gnu"
            outputName = "libwgpu_renderer.so"
        }
        create("linux-x86_64") {
            target = "x86_64-unknown-linux-gnu"
            outputName = "libwgpu_renderer.so"
        }
    }
}