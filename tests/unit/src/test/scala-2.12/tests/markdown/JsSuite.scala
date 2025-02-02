package tests.markdown

import mdoc.internal.cli.Settings
import scala.meta.io.Classpath
import tests.markdown.StringSyntax._

class JsSuite extends BaseMarkdownSuite {
  // NOTE(olafur) Optimization. Cache settings to reuse the Scala.js compiler instance.
  // By default, we create new modifiers for each unit test, which is usually fast.
  override lazy val baseSettings: Settings = super.baseSettings.copy(
    site = super.baseSettings.site ++ Map(
      "js-opt" -> "fast"
    )
  )

  def suffix(name: String): String =
    s"""|<script type="text/javascript" src="$name.md.js" defer></script>
        |
        |<script type="text/javascript" src="mdoc.js" defer></script>
        |""".stripMargin

  check(
    "basic",
    """
      |```scala mdoc:js
      |println("hello world!")
      |```
    """.stripMargin,
    """|```scala
       |println("hello world!")
       |```
       |
       |<div id="mdoc-js-run0" data-mdoc-js></div>
       |
       |<script type="text/javascript" src="basic.md.js" defer></script>
       |
       |<script type="text/javascript" src="mdoc.js" defer></script>
    """.stripMargin
  )

  checkError(
    "error",
    """
      |```scala mdoc:js
      |val x: Int = ""
      |```
    """.stripMargin,
    """
      |error: error.md:3:14: type mismatch;
      | found   : String("")
      | required: Int
      |val x: Int = ""
      |             ^^
    """.stripMargin
  )

  check(
    "multi",
    """
      |```scala mdoc:js
      |println("hello 1!")
      |```
      |
      |```scala mdoc:js
      |println("hello 2!")
      |```
    """.stripMargin,
    """|```scala
       |println("hello 1!")
       |```
       |
       |<div id="mdoc-js-run0" data-mdoc-js></div>
       |
       |```scala
       |println("hello 2!")
       |```
       |
       |<div id="mdoc-js-run1" data-mdoc-js></div>
       |
       |<script type="text/javascript" src="multi.md.js" defer></script>
       |
       |<script type="text/javascript" src="mdoc.js" defer></script>
       |""".stripMargin
  )

  checkError(
    "edit",
    """
      |```scala mdoc:js
      |val x: Int = ""
      |```
      |
      |```scala mdoc:js
      |val y: String = 42
      |```
    """.stripMargin,
    """|error: edit.md:3:14: type mismatch;
       | found   : String("")
       | required: Int
       |val x: Int = ""
       |             ^^
       |error: edit.md:7:17: type mismatch;
       | found   : Int(42)
       | required: String
       |val y: String = 42
       |                ^^
    """.stripMargin
  )

  checkError(
    "isolated",
    """
      |```scala mdoc:js
      |val x = 1
      |```
      |
      |```scala mdoc:js
      |println(x)
      |```
    """.stripMargin,
    """|error: isolated.md:7:9: not found: value x
       |println(x)
       |        ^
    """.stripMargin
  )

  checkCompiles(
    "mountNode",
    """
      |```scala mdoc:js
      |node.innerHTML = "<h3>Hello world!</h3>"
      |```
    """.stripMargin
  )

  check(
    "shared",
    """
      |```scala mdoc:js:shared
      |val x = 1
      |```
      |
      |```scala mdoc:js
      |println(x)
      |```
    """.stripMargin,
    """|```scala
       |val x = 1
       |```
       |
       |```scala
       |println(x)
       |```
       |
       |<div id="mdoc-js-run1" data-mdoc-js></div>
       |
       |<script type="text/javascript" src="shared.md.js" defer></script>
       |
       |<script type="text/javascript" src="mdoc.js" defer></script>
    """.stripMargin
  )

  check(
    "compile-only",
    """
      |```scala mdoc:compile-only
      |println(42)
      |```
    """.stripMargin,
    """|```scala
       |println(42)
       |```
    """.stripMargin
  )

  checkError(
    "compile-only-error",
    """
      |```scala mdoc:compile-only
      |val x: String = 42
      |```
    """.stripMargin,
    """|error: compile-only-error.md:3:17: type mismatch;
       | found   : Int(42)
       | required: String
       |val x: String = 42
       |                ^^
    """.stripMargin
  )

  // It's easy to mess up stripMargin multiline strings when generating code with strings.
  check(
    "stripMargin",
    """
      |```scala mdoc:js
      |val x = '''
      |  |a
      |  | b
      |  |  c
      | '''.stripMargin
      |```
    """.stripMargin.triplequoted,
    s"""|```scala
        |val x = '''
        |  |a
        |  | b
        |  |  c
        | '''.stripMargin
        |```
        |
        |<div id="mdoc-js-run0" data-mdoc-js></div>
        |
        |${suffix("stripMargin")}
        |""".stripMargin.triplequoted
  )

  check(
    "invisible",
    """
      |```scala mdoc:js:invisible
      |println("Hello!")
      |```
    """.stripMargin,
    s"""|
        |<div id="mdoc-js-run0" data-mdoc-js></div>
        |
        |${suffix("invisible")}
        |""".stripMargin
  )

  checkCompiles(
    "deps",
    """
      |```scala mdoc:js
      |println(jsdocs.ExampleJS.greeting)
      |```
    """.stripMargin
  )

  checkError(
    "no-dom",
    """
      |```scala mdoc:js
      |println(jsdocs.ExampleJS.greeting)
      |```
    """.stripMargin,
    """|error: no-dom.md:3 (mdoc generated code) object scalajs is not a member of package org
       |def run0(node: _root_.org.scalajs.dom.raw.Element): Unit = {
       |                          ^
    """.stripMargin,
    settings = {
      val noScalajsDom = Classpath(baseSettings.site("js-classpath")).entries
        .filterNot(_.toNIO.getFileName.toString.contains("scalajs-dom"))
      baseSettings.copy(
        site = baseSettings.site.updated("js-classpath", Classpath(noScalajsDom).syntax)
      )
    }
  )

  checkError(
    "mods-error",
    """|
       |```scala mdoc:js:shared:not
       |println(1)
       |```
    """.stripMargin,
    """|error: mods-error.md:2:25: invalid modifier 'not'
       |```scala mdoc:js:shared:not
       |                        ^^^
    """.stripMargin
  )

  check(
    "commonjs",
    """
      |```scala mdoc:js
      |println("Hello!")
      |```
    """.stripMargin,
    """|```scala
       |println("Hello!")
       |```
       |
       |<div id="mdoc-js-run0" data-mdoc-js></div>
       |
       |<script type="text/javascript" src="mdoc-library.js" defer></script>
       |
       |<script type="text/javascript" src="mdoc-loader.js" defer></script>
       |
       |<script type="text/javascript" src="commonjs.md.js" defer></script>
       |
       |<script type="text/javascript" src="mdoc.js" defer></script>
       |""".stripMargin,
    settings = {
      val libraries = List(
        createTempFile("mdoc-loader.js"),
        createTempFile("mdoc-ignoreme.md"),
        createTempFile("mdoc-library.js"),
        createTempFile("mdoc-library.js.map")
      )
      baseSettings.copy(
        site = baseSettings.site
          .updated("js-module-kind", "CommonJSModule")
          .updated("js-libraries", Classpath(libraries).syntax)
      )
    }
  )

  def unpkgReact =
    """<script crossorigin src="https://unpkg.com/react@16.5.1/umd/react.production.min.js"></script>"""
  check(
    "html-header",
    """
      |```scala mdoc:js:invisible
      |println("Hello!")
      |```
    """.stripMargin,
    s"""|
        |<div id="mdoc-js-run0" data-mdoc-js></div>
        |
        |$unpkgReact
        |
        |<script type="text/javascript" src="html-header.md.js" defer></script>
        |
        |<script type="text/javascript" src="mdoc.js" defer></script>
        |""".stripMargin,
    settings = {
      baseSettings.copy(
        site = baseSettings.site.updated("js-html-header", unpkgReact)
      )
    }
  )
}
