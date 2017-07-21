SUMMARY = "JavaScript framework for robotics, drones, and the Internet of Things (IoT) using Node.js"
HOMEPAGE = "http://cylonjs.com"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=923fb5d3cace36f0a33011d884f7e554"

SRC_URI = "npm://registry.npmjs.org;name=cylon;version=${PV}"

inherit npm

# Must be set after inherit npm since that itself sets S
S = "${WORKDIR}/npmpkg"
LICENSE_${PN} = "Apache-2.0"
