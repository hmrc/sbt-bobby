
with import <nixpkgs> {};

mkShell {
  buildInputs = [
    jdk11
  ];
  shellHook = ''
    export JAVA_HOME=${jdk11}
  '';
}
