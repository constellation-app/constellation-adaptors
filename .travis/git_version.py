#
# Look for module versions in manifest.mf files, and potentially update them.
#
# To use NetBeans auto-update, the module version numbers must be updated.
# We don't want to do a blanket "update everything", because only the modules
# that have changed should have their versions updated.
# The time stamp of the latest build is used as a version number.
#

import os
import os.path
import subprocess
import sys
import getopt
import datetime

MANIFEST = '__manifest__'
PROJECT_DIR = '__projdir__'

MF_VERSION = 'OpenIDE-Module-Specification-Version'
MF_BUNDLE = 'OpenIDE-Module-Localizing-Bundle'
B_MODULE_NAME = 'OpenIDE-Module-Name'

def getManifest(projectDir):
    m = None

    manifest = os.path.join(projectDir, 'manifest.mf')
    if os.path.isfile(manifest):
        m = {MANIFEST:manifest, PROJECT_DIR:projectDir}
        f = open(manifest)
        for line in f:
            line = line.strip()
            if line:
                p = line.index(':')
                key = line[:p]
                value = line[p+1:].lstrip()
                m[key] = value

        f.close()

    return m

def updateManifest(projectDir, version):
    '''Update a manifest with a new specification version.'''

    lines = []
    manifest = os.path.join(projectDir, 'manifest.mf')
    if os.path.isfile(manifest):
        f = open(manifest)
        for line in f:
            line = line.strip()
            lines.append(line)

        f.close()

        f = open(manifest, 'w')
        for line in lines:
            if line.startswith(MF_VERSION+':'):
                p = line.index(' ')
                line = line[:p+1] + version
            f.write('%s\n' % line)

        f.close()

def getBundle(manifest):
    projectDir = manifest[PROJECT_DIR]
    print "Processing: "+projectDir
    if MF_BUNDLE not in manifest:
        print "Skipping %s" % projectDir
        return None
    else:
        bundleName = manifest[MF_BUNDLE]
        bundle = os.path.join(projectDir, 'src', bundleName)
        b = {}
        f = open(bundle)
        fullLine = ''
        for line in f:
          line = line.strip()
          if line:
              if line.endswith('\\'):
                  fullLine += line[:-1]
              else:
                  fullLine += line
                  p = fullLine.index('=')
                  key = fullLine[:p]
                  value = fullLine[p+1:]
                  b[key] = value

                  fullLine = ''

        f.close()

    return b

def getGitChangeSetTimeStamp(path):
    '''Use Git to find out the datetime of the last changeset for a module'''
    '''Returns a formatted timestamp to use as a version number'''
    cmd = ['git','log', '-1', '--walk-reflogs', '--pretty=format:%ct', '--', path]
    # print cmd
    p = subprocess.Popen(cmd, bufsize=0, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out,err = p.communicate()
    if err:
        print '!!'
        print '!!', err
        print '!!'
        out = None
    if not out:
        cmd = ['git','log', '-1', '--pretty=format:%ct', '--', path]
        # print cmd
        p = subprocess.Popen(cmd, bufsize=0, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out,err = p.communicate()
        if err:
            print '!!'
            print '!!', err
            print '!!'
            out = None
    print 'UNIX timestamp: %s' % out
    out = datetime.datetime.fromtimestamp(float(out))
    out = out.strftime("%Y%m%d.%H%M%S")

    return out

def getNextVersion(version,build_number):
    '''Update a version number of the form a.b.c... by adding a build number.

    If the version number has less than two parts, it will be extended
    to two parts.'''

    v = version.split('.')

    v = v[:3]

    while len(v)<3:
        v.append('0')

    v[0] = '2'

    v[-1] = str(build_number)

    return '.'.join(v)

def main(argv):
  build_number = ''
  force_update = ''

  try:
    opts, args = getopt.getopt(argv,"hu:",["force-update="])
  except getopt.GetoptError:
    print 'Usage:\n%s -b <build-number> -u <force_update [y/N]>' % sys.argv[0]
    sys.exit(2)
  for opt, arg in opts:
    if opt == 'h':
      print 'Usage:\n%s -b <build-number> -u <force_update [y/N]>' % sys.argv[0]
      sys.exit(1)
    elif opt in ("-u", "--force-update"):
      force_update = arg

  print 'Build Number is "', build_number, '"'

  # Get a list of all directories in the current directory
  moduleDirectories = set()
  dirs = os.listdir(os.curdir)
  for dir in dirs:
    if os.path.isdir(dir):
      if dir[0] != ".":
        moduleDirectories.add(dir)


  # Find any manifests for the projects in those directories.
  #
  manifests = []
  for directory in moduleDirectories:
      m = getManifest(directory)
      if m:
          manifests.append(m)

  # Get an ordered list of module names.
  # Don't include any modules that aren't from package au.gov.*
  modules = {}
  for m in manifests:
      b = getBundle(m)
      if b is None:
        continue

      if "au.gov." in m['OpenIDE-Module']:

        modules[b[B_MODULE_NAME]] = m

  # Display the results.
  #
  for mn in sorted(modules.keys()):
      module = modules[mn]
      version = module[MF_VERSION]
      if version:
          b = getBundle(module)
          moduleName = b[B_MODULE_NAME]
          print '%-25s %s %s (next version is %s)' % (moduleName, module[PROJECT_DIR], version, getNextVersion(version, getGitChangeSetTimeStamp(module[PROJECT_DIR])))

  # Update?
  #
  if force_update == '':
    sys.stdout.write('Update [y/N]? ')
    sys.stdout.flush()
    force_update = sys.stdin.readline().strip()


  if force_update.upper()=='Y':
      for m in modules:
          module = modules[m]
          projDir = module[PROJECT_DIR]
          versionNext = getNextVersion(module[MF_VERSION], getGitChangeSetTimeStamp(projDir))
          print projDir, versionNext
          updateManifest(projDir, versionNext)

if __name__ == "__main__":
  main(sys.argv[1:])
