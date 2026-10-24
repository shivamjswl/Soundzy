# Corresponding library source

`jump3r-1.0.5-sources.jar` is the Maven Central source artifact for `de.sciss:jump3r:1.0.5`. Upstream LGPL-2.1-or-later headers are retained; the license text is in `licenses/LGPL-2.1.txt`.

`desugar_jdk_libs-2.1.5-source.zip` contains the upstream source tree at release-preparation commit **73170c345e6a762fc6a1f0301bb15218850023ef** from https://github.com/google/desugar_jdk_libs. `VERSION_JDK11.txt` declares version 2.1.5 and `DEPENDENCIES_JDK11.txt` declares its matching configuration artifact. The archive was generated with `git archive --format=zip --prefix=desugar_jdk_libs-2.1.5/ 73170c345e6a762fc6a1f0301bb15218850023ef`; its checksum is supplied alongside it. Source, build instructions and upstream licensing material remain intact. Copies of LICENSE, ADDITIONAL_LICENSE_INFO and ASSEMBLY_EXCEPTION are under `licenses/`. The app uses the unmodified Maven runtime artifact and AGP's core library desugaring, not a locally rebuilt library.

The full Soundzy source download also includes pinned NewPipeExtractor and nanojson source under `.deps/`. Application and wrapper build instructions are in README.md and DEVELOPMENT.md. Local signing keys and credentials are intentionally excluded from source archives.
