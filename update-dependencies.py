#!/usr/bin/env python3

import os
import subprocess
from termcolor import colored

from siphon.base.utils import cd, get_latest_version, version_dir_path, yn
from siphon_dependencies import Dependencies

def npm_install(dir):
    # Runs 'npm install' in a given directory
    with cd(dir):
        subprocess.call(['npm', 'install'])

def main():
    # TODO Add base-version argument. Default to the latest one for now
    latest_version = get_latest_version()
    pkg_dir = os.path.join(version_dir_path(latest_version), 'lib')
    pkg_path = os.path.join(pkg_dir, 'package.json')

    print(colored('Fetching latest dependencies...', 'yellow'))
    dependencies = Dependencies(latest_version)
    dependencies.update_package_file(pkg_path)

    update_modules = yn(colored('Would you like to update your local ' \
                                'node_modules? [Y/n] ', 'yellow'))
    if update_modules:
        npm_install(pkg_dir)

    print(colored('Warning: If react-native has been updated, you will ' \
          'need to update the Podfile of the appropriate project and ' \
          'install.', 'red'))

if __name__ == '__main__':
    main()
