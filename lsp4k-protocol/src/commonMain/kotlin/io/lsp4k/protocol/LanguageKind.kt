package io.lsp4k.protocol

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Predefined language kinds (the `languageId` of a text document). The set is open: any string
 * is a valid language kind, and the constants below are only the identifiers the specification
 * predefines.
 */
@Serializable
@JvmInline
public value class LanguageKind(
    public val value: String,
) {
    public companion object {
        public val ABAP: LanguageKind = LanguageKind("abap")
        public val WindowsBat: LanguageKind = LanguageKind("bat")
        public val BibTeX: LanguageKind = LanguageKind("bibtex")
        public val Clojure: LanguageKind = LanguageKind("clojure")
        public val Coffeescript: LanguageKind = LanguageKind("coffeescript")
        public val C: LanguageKind = LanguageKind("c")
        public val CPP: LanguageKind = LanguageKind("cpp")
        public val CSharp: LanguageKind = LanguageKind("csharp")
        public val CSS: LanguageKind = LanguageKind("css")
        public val D: LanguageKind = LanguageKind("d")
        public val Delphi: LanguageKind = LanguageKind("pascal")
        public val Diff: LanguageKind = LanguageKind("diff")
        public val Dart: LanguageKind = LanguageKind("dart")
        public val Dockerfile: LanguageKind = LanguageKind("dockerfile")
        public val Elixir: LanguageKind = LanguageKind("elixir")
        public val Erlang: LanguageKind = LanguageKind("erlang")
        public val FSharp: LanguageKind = LanguageKind("fsharp")
        public val GitCommit: LanguageKind = LanguageKind("git-commit")
        public val GitRebase: LanguageKind = LanguageKind("git-rebase")
        public val Go: LanguageKind = LanguageKind("go")
        public val Groovy: LanguageKind = LanguageKind("groovy")
        public val Handlebars: LanguageKind = LanguageKind("handlebars")
        public val Haskell: LanguageKind = LanguageKind("haskell")
        public val HTML: LanguageKind = LanguageKind("html")
        public val Ini: LanguageKind = LanguageKind("ini")
        public val Java: LanguageKind = LanguageKind("java")
        public val JavaScript: LanguageKind = LanguageKind("javascript")
        public val JavaScriptReact: LanguageKind = LanguageKind("javascriptreact")
        public val JSON: LanguageKind = LanguageKind("json")
        public val LaTeX: LanguageKind = LanguageKind("latex")
        public val Less: LanguageKind = LanguageKind("less")
        public val Lua: LanguageKind = LanguageKind("lua")
        public val Makefile: LanguageKind = LanguageKind("makefile")
        public val Markdown: LanguageKind = LanguageKind("markdown")
        public val ObjectiveC: LanguageKind = LanguageKind("objective-c")
        public val ObjectiveCPP: LanguageKind = LanguageKind("objective-cpp")
        public val Pascal: LanguageKind = LanguageKind("pascal")
        public val Perl: LanguageKind = LanguageKind("perl")
        public val Perl6: LanguageKind = LanguageKind("perl6")
        public val PHP: LanguageKind = LanguageKind("php")
        public val Plaintext: LanguageKind = LanguageKind("plaintext")
        public val Powershell: LanguageKind = LanguageKind("powershell")
        public val Pug: LanguageKind = LanguageKind("jade")
        public val Python: LanguageKind = LanguageKind("python")
        public val R: LanguageKind = LanguageKind("r")
        public val Razor: LanguageKind = LanguageKind("razor")
        public val Ruby: LanguageKind = LanguageKind("ruby")
        public val Rust: LanguageKind = LanguageKind("rust")
        public val SCSS: LanguageKind = LanguageKind("scss")
        public val SASS: LanguageKind = LanguageKind("sass")
        public val Scala: LanguageKind = LanguageKind("scala")
        public val ShaderLab: LanguageKind = LanguageKind("shaderlab")
        public val ShellScript: LanguageKind = LanguageKind("shellscript")
        public val SQL: LanguageKind = LanguageKind("sql")
        public val Swift: LanguageKind = LanguageKind("swift")
        public val TypeScript: LanguageKind = LanguageKind("typescript")
        public val TypeScriptReact: LanguageKind = LanguageKind("typescriptreact")
        public val TeX: LanguageKind = LanguageKind("tex")
        public val VisualBasic: LanguageKind = LanguageKind("vb")
        public val XML: LanguageKind = LanguageKind("xml")
        public val XSL: LanguageKind = LanguageKind("xsl")
        public val YAML: LanguageKind = LanguageKind("yaml")
    }
}
