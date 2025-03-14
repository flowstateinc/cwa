((nil
  . ((indent-tabs-mode . nil)
     (require-final-newline . t)))

 (clojure-mode
  . ((cider-clojure-cli-aliases . ":dev:logging:test")
     (cider-ns-refresh-after-fn . "web.dev/after-refresh")
     (cider-ns-refresh-before-fn . "web.dev/before-refresh")
     (cider-preferred-build-tool . clojure-cli)
     (cider-redirect-server-output-to-repl . nil)
     (cider-test-default-exclude-selectors . ("kaocha/pending"))
     (cljr-favor-prefix-notation . nil)
     (cljr-insert-newline-after-require . t)
     (clojure-indent-style . always-align))))
