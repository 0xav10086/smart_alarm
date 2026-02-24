package io.github.av10086.smartalarm

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.annotations.XposedHooker
import io.github.libxposed.api.annotations.XposedInit
import io.github.libxposed.api.annotations.XposedModuleDetail

@XposedModuleDetail(name = "Smart Alarm", scope = ["nodomain.freeyourgadget.gadgetbridge"])
class ModuleMain @XposedInit constructor(
    iface: XposedInterface,
    param: XposedModule.ModuleLoadedParam) : XposedModule(iface, param) {
    override fun onPackageLoaded(param: XposedModule.PackageLoadedParam) {
        super.onPackageLoaded(param)
        if (param.packageName == "nodomain.freeyourgadget.gadgetbridge") {
            log("Smart Alarm hooked into Gadgetbridge.")
            // In a real scenario, you would place your hooks here.
            // For now, we just log a message.
        }
    }
}
