DESCRIPTION = "nodeJS Evented I/O for V8 JavaScript"
HOMEPAGE = "http://nodejs.org"
LICENSE = "MIT & BSD-2-Clause & BSD-3-Clause & ISC & CC-BY-3.0 & AFL-2.1 & Apache-2.0 & OpenSSL & Zlib & Artistic-2.0 & (BSD-3-Clause | GPLv2)"
LIC_FILES_CHKSUM = "file://LICENSE;md5=41a3a0ccf7f515cac3377389dd8faac8"

DEPENDS = "openssl"
DEPENDS_append_class-target = " nodejs-native"

COMPATIBLE_MACHINE_armv4 = "(!.*armv4).*"
COMPATIBLE_MACHINE_armv5 = "(!.*armv5).*"
COMPATIBLE_MACHINE_mips64 = "(!.*mips64).*"

SRC_URI = "http://nodejs.org/dist/v${PV}/node-v${PV}.tar.xz;name=node \
           http://nodejs.org/dist/v${PV}/node-v${PV}-headers.tar.gz;name=node-headers;unpack=false \
"
SRC_URI_append_quark = "file://0001-nodejs-add-compile-flag-options-for-quark.patch"
SRC_URI_append_intel-quark = "file://0001-nodejs-add-compile-flag-options-for-quark.patch"

SRC_URI[node.md5sum] = "301c9afa4848561173d1dbe017d03636"
SRC_URI[node.sha256sum] = "02ba35391edea2b294c736489af01954ce6e6c39d318f4423ae6617c69ef0a51"
SRC_URI[node-headers.md5sum] = "d68f6bf03812ddab676d7e2313c29413"
SRC_URI[node-headers.sha256sum] = "a82caf153b7649656bce64dec40d136008babbef419e35b2a83242049de44b23"

S = "${WORKDIR}/node-v${PV}"

# v8 errors out if you have set CCACHE
CCACHE = ""

def map_nodejs_arch(a, d):
    import re

    if   re.match('i.86$', a): return 'ia32'
    elif re.match('x86_64$', a): return 'x64'
    elif re.match('aarch64$', a): return 'arm64'
    elif re.match('powerpc64$', a): return 'ppc64'
    elif re.match('powerpc$', a): return 'ppc'
    return a

ARCHFLAGS_arm = "${@bb.utils.contains('TUNE_FEATURES', 'callconvention-hard', '--with-arm-float-abi=hard', '--with-arm-float-abi=softfp', d)}"
GYP_DEFINES_append_mipsel = " mips_arch_variant='r1' "
ARCHFLAGS ?= ""

# Node is way too cool to use proper autotools, so we install two wrappers to forcefully inject proper arch cflags to workaround gypi
do_configure () {
    export LD="${CXX}"
    GYP_DEFINES="${GYP_DEFINES}" export GYP_DEFINES
    # $TARGET_ARCH settings don't match --dest-cpu settings
   ./configure --prefix=${prefix} --without-snapshot --shared-openssl \
               --dest-cpu="${@map_nodejs_arch(d.getVar('TARGET_ARCH'), d)}" \
               --dest-os=linux \
               --without-intl \
               ${ARCHFLAGS}
}

do_compile () {
    export LD="${CXX}"
    oe_runmake BUILDTYPE=Release
}

do_install () {
    oe_runmake install DESTDIR=${D}
}

do_install_append_class-native() {
    # use node from PATH instead of absolute path to sysroot
    # node-v0.10.25/tools/install.py is using:
    # shebang = os.path.join(node_prefix, 'bin/node')
    # update_shebang(link_path, shebang)
    # and node_prefix can be very long path to bindir in native sysroot and
    # when it exceeds 128 character shebang limit it's stripped to incorrect path
    # and npm fails to execute like in this case with 133 characters show in log.do_install:
    # updating shebang of /home/jenkins/workspace/build-webos-nightly/device/qemux86/label/open-webos-builder/BUILD-qemux86/work/x86_64-linux/nodejs-native/0.10.15-r0/image/home/jenkins/workspace/build-webos-nightly/device/qemux86/label/open-webos-builder/BUILD-qemux86/sysroots/x86_64-linux/usr/bin/npm to /home/jenkins/workspace/build-webos-nightly/device/qemux86/label/open-webos-builder/BUILD-qemux86/sysroots/x86_64-linux/usr/bin/node
    # /usr/bin/npm is symlink to /usr/lib/node_modules/npm/bin/npm-cli.js
    # use sed on npm-cli.js because otherwise symlink is replaced with normal file and
    # npm-cli.js continues to use old shebang
    sed "1s^.*^#\!/usr/bin/env node^g" -i ${D}${exec_prefix}/lib/node_modules/npm/bin/npm-cli.js
}

do_install_append_class-target() {
    sed "1s^.*^#\!${bindir}/env node^g" -i ${D}${exec_prefix}/lib/node_modules/npm/bin/npm-cli.js

    # install node-gyp node hedaers in /usr/include/node-gyp/
    cd ${D}/${exec_prefix}/lib/node_modules/npm/node_modules/node-gyp/
    export HOME=${D}/usr/include/node-gyp
    sed -i 's/\.node-gyp//' lib/node-gyp.js

    ${STAGING_BINDIR_NATIVE}/node bin/node-gyp.js --verbose install --tarball=${DL_DIR}/node-v${PV}-headers.tar.gz
}

pkg_postinst_${PN} () {
    sed -e '/^PATH=/aNODE_PATH=\/usr\/lib\/node_modules\/' \
        -e 's/\(^export\)\(.*\)/\1 NODE_PATH\2/' \
        -i $D/etc/profile
}

pkg_prerm_${PN} () {
    sed -e '/^NODE_PATH.*$/d' \
        -e 's/\(^export\)\(.*\)\(\<NODE_PATH\>\s\)\(.*\)/\1\2\4/' \
        -i $D/etc/profile
}

PACKAGES =+ "${PN}-npm"
FILES_${PN}-npm = "${exec_prefix}/lib/node_modules ${bindir}/npm"
RDEPENDS_${PN}-npm = "bash python-shell python-datetime python-subprocess python-textutils \
                      python-netclient python-ctypes python-misc python-compiler python-multiprocessing"

PACKAGES =+ "${PN}-systemtap"
FILES_${PN}-systemtap = "${datadir}/systemtap"

BBCLASSEXTEND = "native"
