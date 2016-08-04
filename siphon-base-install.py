#!/usr/bin/env python3
import os
import subprocess
import shutil

from siphon.base.utils import cd
from siphon.base.utils import version_dir_path, project_dir_path, get_versions

SCRUB_FROM_NPM_INSTALL = [
    'react-native/jestSupport',
    'react-native/packager',
    'react-native/local-cli',
    'react-native/node_modules',
    'react-native/ReactAndroid',
]

def dependencies_installed(version):
    relative_path = 'lib/third-party/node_modules'
    node_modules_dir = os.path.join(version_dir_path(version), relative_path)
    return os.path.isdir(node_modules_dir)

def main():
    print('Installing base dependencies...')
    versions = get_versions()
    for v in versions:
        print('\n[Installing dependencies for base v%s]' % v)

        if not dependencies_installed(v):
            relative_path = 'lib/'
            install_dir = os.path.join(version_dir_path(v), relative_path)
            node_modules_dir = os.path.join(install_dir, 'node_modules')
            with cd(install_dir):
                subprocess.check_call(['npm', 'install', '--loglevel', 'http'])

            with cd(node_modules_dir):
                # Scrub superfluous directories installed
                print('Scrubbing installed modules...')
                for d in SCRUB_FROM_NPM_INSTALL:
                    if os.path.isdir(d):
                        shutil.rmtree(d, ignore_errors=True)
        else:
            print('Dependencies already installed')

if __name__ == '__main__':
    main()
