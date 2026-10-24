{
  description = "NixOS-first Java 25 / Cleanroom development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs = { nixpkgs, ... }: let
    system = "x86_64-linux";
    pkgs = import nixpkgs { inherit system; };
    nativeLibraries = with pkgs; [
      libGL
      alsa-lib
      libpulseaudio
      udev
      wayland
      libxkbcommon
      libdecor
      flite
    ];
  in {
    devShells.${system}.default = pkgs.mkShell {
      packages = [ pkgs.jdk25 pkgs.git ];
      JAVA_HOME = "${pkgs.jdk25}/lib/openjdk";

      # Use the running system's GPU drivers.
      shellHook = ''
        export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath nativeLibraries}:/run/opengl-driver/lib''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
      '';
    };
  };
}
