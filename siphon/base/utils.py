
import os
import re
import shutil
import tempfile
import subprocess
from contextlib import contextmanager
from decimal import Decimal

REPO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../')
XCODE_VERSIONS_PATH = os.path.join(REPO_PATH, 'versions')

class InvalidVersion(Exception):
    """ Exception corresponding to an invalid version being provided """
    def __init__(self, tag):
        self.tag = tag

    def __str__(self):
        return self.tag

def version_dir_path(version):
    """
    Takes a version string (e.g. '0.3') and returns the corresponding
    version directory.
    """
    return os.path.join(XCODE_VERSIONS_PATH, version)

def project_dir_path(version):
    """
    Takes a version string (e.g. '0.3') and returns the corresponding
    project directory.
    """
    relative_to_version_dir = '%s/base-project/ios' % version
    return os.path.join(XCODE_VERSIONS_PATH, relative_to_version_dir)

def get_versions(directory=XCODE_VERSIONS_PATH):
    """ Returns a list of versions that we support """
    with cd(directory):
        cwd = os.getcwd()
        # Compile a list of available versions
        available_versions = [f for f in os.listdir(cwd)
                              if not f.startswith('.')]
        available_versions.sort(key=lambda f: Decimal(f), reverse=True)
    return available_versions

def get_latest_version(directory=XCODE_VERSIONS_PATH):
    """ Returns the latest base version that we support """
    available_versions = get_versions(directory)
    return available_versions[0]

def version_exists(version):
    """ Takes a base version and checks if it is supported """
    version_dir = version_dir_path(version)
    return os.path.isdir(version_dir)

@contextmanager
def make_temp_dir(suffix=''):
    """ Use this within a `with` statement. Cleans up after itself. """
    path = tempfile.mkdtemp(suffix=suffix)
    try:
        yield path
    finally:
        shutil.rmtree(path)

def bash(cmd):
    print('[bash: "%s"]' % cmd)
    return subprocess.call('/bin/bash -c "%s"' % cmd, shell=True)

def yn(msg):
    try:
        valid_response = False
        while not valid_response:
            response = input(msg) or 'y'
            if response == 'y' or response == 'Y':
                return True
            elif response == 'n' or response == 'N':
                return False
            else:
                msg = 'Please enter \'y\' or \'n\': '
    except KeyboardInterrupt:
        return False

@contextmanager
def cd(*paths):
    path = os.path.join(*paths)
    old_path = os.getcwd()
    print('[cd: %s]' % path)
    os.chdir(path)
    try:
        yield
    finally:
        print('[cd: %s]' % old_path)
        os.chdir(old_path)
