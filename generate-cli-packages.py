#!/usr/bin/env python3
import os
import sys
import argparse
import tarfile

from siphon.base.utils import get_versions, version_dir_path

# For generating the source archive
IGNORE_EXTENSIONS = ('.pyc', '.DS_Store', '.podspec')
IGNORE_FILES = ('Podfile', 'Podfile.lock', 'Manifest.lock', 'README.md')
IGNORE_DIRECTORIES = ('Build', 'Local Podspecs')
KEEP_DIRECTORIES = ('SiphonSDK/Build',)

def contains_any(string, substrings):
    return any([string.find(sub) != -1 for sub in substrings])

def base_version_paths(version):
    paths = []
    version_root = version_dir_path(version)
    for root, dirs, files in os.walk(version_root):
        dir_abs = os.path.abspath(root) # Absolute path to the directory
        dir_rel = dir_abs[len(version_root):] # Relative to the version root
        for f in files:
            if not f.endswith(IGNORE_EXTENSIONS) and \
                f not in IGNORE_FILES and \
                not any([s in dir_rel.split('/') for s in IGNORE_DIRECTORIES]):
                paths.append(os.path.join(dir_abs, f))
            elif contains_any(dir_rel, KEEP_DIRECTORIES):
                paths.append(os.path.join(dir_abs, f))
    return paths

def archive_base_versions(dest):
    versions = get_versions()
    for v in versions:
        # Get the absolute destination path
        if dest[0] != '/':
            dest_path = os.path.join(os.getcwd(), dest)
        else:
            dest_path = dest
        src = 'siphon-base-%s.tar.gz' % v
        archive = os.path.join(dest, src)
        proj_paths = base_version_paths(v) # absolute paths
        with tarfile.open(archive, 'w:gz') as tf:
            for path in proj_paths:
                # Add
                rel_path = os.path.relpath(path, version_dir_path(v))
                tf.add(path, rel_path)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dest', type=str, help='Directory to store output.')

    args = parser.parse_args()

    if not args.dest:
        parser.print_usage(file=sys.stderr)
        sys.exit(1)
    elif os.path.isfile(args.dest):
        sys.stderr.write('Destination is a file: %s\n' % args.dest)
        sys.exit(1)

    if not os.path.isdir(args.dest):
        print('Creating destination directory: %s' % args.dest)
        os.makedirs(args.dest)

    archive_base_versions(args.dest)

if __name__ == '__main__':
    main()
