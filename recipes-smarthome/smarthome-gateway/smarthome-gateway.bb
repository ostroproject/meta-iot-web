SUMMARY = "SmartHome Gateway"
DESCRIPTION = "SmartHome webserver running on the home gateway"
HOMEPAGE = "https://github.com/01org/SmartHome-Demo/"
LICENSE = "Apache-2.0"

LIC_FILES_CHKSUM = "file://COPYING;md5=82d0338d6e61d25fb51cabb1504c0df6"

DEPENDS = "nodejs-native iotivity iotivity-node"
RDEPENDS_${PN} += "iotivity node-mraa nodejs iotivity-node"

SRC_URI = "git://git@github.com/01org/SmartHome-Demo.git;protocol=https \
           file://0001-Remove-iotivity-node-dependency.patch \
           file://smarthome-gateway.service \
          "

SRCREV = "f59656556f2ac126bf9ebee155a380fe655caafd"
PV = "0.1+git${SRCPV}"

S = "${WORKDIR}/git/"

inherit systemd
SYSTEMD_SERVICE_${PN} = "smarthome-gateway.service"
INSANE_SKIP_${PN} += "ldflags staticdev"

do_compile () {
    # changing the home directory to the working directory, the .npmrc will be created in this directory
    export HOME=${WORKDIR}

    # does not build dev packages
    npm config set dev false

    # access npm registry using http
    npm set strict-ssl false
    npm config set registry http://registry.npmjs.org/

    # configure http proxy if neccessary
    if [ -n "${http_proxy}" ]; then
        npm config set proxy ${http_proxy}
    fi
    if [ -n "${HTTP_PROXY}" ]; then
        npm config set proxy ${HTTP_PROXY}
    fi

    # configure cache to be in working directory
    npm set cache ${WORKDIR}/npm_cache

    # clear local cache prior to each compile
    npm cache clear

    case ${TARGET_ARCH} in
        i?86) targetArch="ia32"
            echo "targetArch = 32"
            ;;
        x86_64) targetArch="x64"
            echo "targetArch = 64"
            ;;
        arm) targetArch="arm"
            ;;
        aarch64) targetArch="arm64"
            ;;
        mips) targetArch="mips"
            ;;
        sparc) targetArch="sparc"
            ;;
        *) echo "unknown architecture"
           exit 1
            ;;
    esac

    # Compile and install node modules in source directory
    npm --arch=${targetArch} --production --verbose install
}

do_install () {
    install -d ${D}${libdir}/node_modules/smarthome-gateway/

    install -m 0644 ${S}/gateway/gateway-server.js ${D}${libdir}/node_modules/smarthome-gateway/gateway-server.js
    install -m 0644 ${S}/package.json ${D}${libdir}/node_modules/smarthome-gateway/package.json

    cp -r ${S}/gateway/rules-engine/ ${D}${libdir}/node_modules/smarthome-gateway/
    cp -r ${S}/gateway/webui/ ${D}${libdir}/node_modules/smarthome-gateway/
    cp -r ${S}/node_modules/ ${D}${libdir}/node_modules/smarthome-gateway/

    # Install SmartHome gateway service script
    install -d ${D}/${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/smarthome-gateway.service ${D}/${systemd_unitdir}/system/
}

FILES_${PN} = "${libdir}/node_modules/smarthome-gateway/ \
               ${systemd_unitdir}/system/ \
              "

INHIBIT_PACKAGE_DEBUG_SPLIT = "1"

PACKAGES = "${PN}"
