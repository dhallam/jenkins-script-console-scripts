/*
    Copyright (c) 2015-2017 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
    the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

/*
   This script completely disables Jenkins CLI and ensures that it will remain
   disabled when Jenkins is restarted.

   - Installs disable-jenkins-cli.groovy script to $JENKINS_HOME/init.groovy.d
   - Evaluates disable-jenkins-cli.groovy to patch the Jenkins runtime so no
     restart is required.
*/

import jenkins.model.Jenkins

/*
   Define available bindings.  The bindings had to be listed first in order for
   recursion to work within closures.
*/
downloadFile = null
sha256sum = null

/**
Download a file to a local `fullpath`.  If the parent directories of the path
are missing then they are automatically created (similar to the Linux command
`mkdir -p`).

USAGE:

    downloadFile('http://example.com', '/tmp/foo/index.html')

PARAMETERS:

* `url` - A `String` which is a URL to a file on a website.
* `fullpath` - A `String` which is a full file path.  It is the destination of
  the downloaded file.

RETURNS:

A `Bolean`, `true` if downloading the file was a success or `false` if not.
*/
downloadFile = { String url, String fullpath ->
    try {
        new File(fullpath).with { file ->
            //make parent directories if they don't exist
            if(!file.getParentFile().exists()) {
                file.getParentFile().mkdirs()
            }
            file.newOutputStream().with { file_os ->
                file_os << new URL(url).openStream()
                file_os.close()
            }
        }
    }
    catch(Exception e) {
        sandscapeErrorLogger(ExceptionUtils.getStackTrace(e))
        return false
    }
    return true
}

/**
Calculate the SHA-256 hash of an object.

USAGE:

    sha256sum('some string')
    sha256sum(new File('path-to-file'))

RETURNS:

A `String`, which is the SHA-256 hex digest of the object passed.
*/
sha256sum = { input ->
    def digest = java.security.MessageDigest.getInstance("SHA-256")
    digest.update( input.bytes )
    new BigInteger(1,digest.digest()).toString(16).padLeft(64, '0')
}

//main method
disable_jenkins_cli_script = "${Jenkins.instance.root}/init.groovy.d/disable-jenkins-cli.groovy".toString()
downloadFile('https://raw.githubusercontent.com/samrocketman/jenkins-script-console-scripts/master/disable-jenkins-cli.groovy', disable_jenkins_cli_script)
new File(disable_jenkins_cli_script).with { f ->
    if(sha256sum(f) == '06defb6916c7b481bb48a34e96a2752de6bffc52e10990dce82be74076e037a4') {
        println "Disable Jenkins CLI script successfully installed to ${disable_jenkins_cli_script} (patch persists on Jenkins restart)."
        try {
            evaluate(f)
            println 'Runtime has been patched to disable Jenkins CLI.  No restart necessary.'
        } catch(Exception e) {
            println "ERROR: Runtime patching has failed.  Removed ${disable_jenkins_cli_script}"
            f.delete()
            throw e
        }
    } else {
        println 'ERROR: Disable Jenkins CLI script checksum mismatch; Aborting.'
        f.delete()
    }
}
null
