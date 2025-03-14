const plugin = require("tailwindcss/plugin");

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./{resources,src}/**/*.clj"],
  plugins: [
    plugin(({ addVariant }) => {
      addVariant("htmx-added", ["&.htmx-added", ".htmx-added &"]);
      addVariant("htmx-request", ["&.htmx-request", ".htmx-request &"]);
      addVariant("htmx-settling", ["&.htmx-settling", ".htmx-settling &"]);
      addVariant("htmx-swapping", ["&.htmx-swapping", ".htmx-swapping &"]);
    }),
  ],
};
