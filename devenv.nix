{pkgs, ...}: {
  packages = with pkgs; [
    # Clojure dependencies
    babashka
    clj-kondo
    cljfmt
    (clojure.override {jdk = pkgs.temurin-jre-bin-23;})
    clojure-lsp

    # Development dependencies
    bash # Because GitHub workflows only include Bash
    curl
    geckodriver
    git
    htmlq
    nodePackages.prettier

    # Infrastructure
    docker

    # Sharp dependencies
    vips
    libavif
    libjpeg
    libpng
    libwebp

    # Binary optimisation
    pngcrush
  ];

  env.DATABASE_URL = "jdbc:postgresql://127.0.0.1:5432/web_dev?user=web&password=please";
  env.PEDESTAL_ENV = "dev";
  env.WEB_IN_NIX = "true";

  languages.javascript.enable = true;
  languages.javascript.pnpm.enable = true;
  languages.javascript.pnpm.package = pkgs.pnpm;

  services.postgres.enable = true;
  services.postgres.extensions = extensions: [
    extensions.pgvector
    extensions.postgis
  ];

  services.postgres.package = pkgs.postgresql_16;

  services.postgres.initialScript = ''
    create role web superuser login password 'please';
  '';

  services.postgres.listen_addresses = "127.0.0.1";
  services.postgres.initialDatabases = [
    {
      name = "web_dev";
      user = "web";
      pass = "please";
    }
    {
      name = "web_test";
      user = "web";
      pass = "please";
    }
  ];

  process.manager.implementation = "process-compose";
  process.managers.process-compose.tui.enable = false;

  processes.nrepl.exec = "bin/nrepl";
  processes.nrepl.process-compose = {
    availability.restart = "always";

    depends_on = {
      postgres.condition = "process_started";
      tailwind.condition = "process_started";
    };
  };

  processes.tailwind.exec = "bin/tailwind";
  processes.tailwind.process-compose = {
    is_tty = true;
  };
}
