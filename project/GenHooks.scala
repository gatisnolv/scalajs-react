import sbt._

object GenHooks {

  def apply(srcRootDir: File): Unit = {

    val dir = srcRootDir / "japgolly/scalajs/react/hooks"

    println()
    println("Generating hooks boilerplate in: " + dir.getAbsolutePath)

    val hookCtx_apply = List.newBuilder[String]
    val hookCtx_Ps    = List.newBuilder[String]
    val hookCtxFns    = List.newBuilder[String]
    val dslAtSteps    = List.newBuilder[String]
    val stepMultis    = List.newBuilder[String]

    for (n <- 1 to 21) {
      val preHns       = (1 until n).map("H" + _).mkString(", ")
      val Hns          = (1 to n).map("H" + _).mkString(", ")
      val coHns        = (1 to n).map("+H" + _).mkString(", ")
      val hookParams   = (1 to n).map(i => s"hook$i: H$i").mkString(", ")
      val hookArgs     = (1 to n).map(i => s"hook$i").mkString(", ")
      val ctxParams    = ((1 until n).map(i => s"hook$i: H$i") :+ s"final val hook$n: H$n").mkString(", ")
      val ctxSuperArgs = (1 until n).map(i => s", hook$i").mkString
      val ctxToStr     = (1 to n).map(i => s",\\n  hook$i = !hook$i").mkString

      hookCtx_apply += s"  def apply[P, $Hns](props: P, $hookParams): P$n[P, $Hns] =\n    new P$n(props, $hookArgs)"

      hookCtx_Ps +=
        s"""  class P$n[+P, $coHns](props: P, $ctxParams) extends P${n-1}(props$ctxSuperArgs) {
           |    override def toString = s"HookCtx(\\n  props = !props$ctxToStr)"
           |    def apply$n[A](f: (P, $Hns) => A): A = f(props, $hookArgs)
           |  }
           |""".stripMargin.replace('!', '$')

      hookCtxFns += s"  sealed trait P$n[P, $Hns] extends HookCtxFn { override type Fn[A] = (P, $Hns) => A }"

      if (n != 1) {
        val s = n - 1

        dslAtSteps += s"  sealed trait AtStep$s[P, $preHns] { type Next[H$n] = DslMulti[P, HookCtx.P$n[P, $Hns], HookCtxFn.P$n[P, $Hns]#Fn] }"

        val preCtxArgs = (1 until n).map(i => s"ctx$s.hook$i").mkString(", ")
        stepMultis +=
          s"""  type AtStep$s[P, $preHns] = To[
             |    P,
             |    HookCtx.P$s[P, $preHns],
             |    HookCtxFn.P$s[P, $preHns]#Fn,
             |    DslMulti.AtStep$s[P, $preHns]#Next]
             |
             |  implicit def atStep$s[P, $preHns]: AtStep$s[P, $preHns] =
             |    new StepMulti[P, HookCtx.P$s[P, $preHns], HookCtxFn.P$s[P, $preHns]#Fn] {
             |      override type Next[H$n] = DslMulti.AtStep$s[P, $preHns]#Next[H$n]
             |      override def next[H$n] =
             |        (renderPrev, initNextHook) => {
             |          val renderNext: RenderFn[P, HookCtx.P$n[P, $Hns]] =
             |            render => renderPrev { ctx$s =>
             |              val h$n = initNextHook(ctx$s)
             |              val ctx$n = HookCtx(ctx$s.props, $preCtxArgs, h$n)
             |              render(ctx$n)
             |            }
             |          new DslMulti(renderNext)
             |        }
             |      override def squash[A] = f => _.apply$s(f)
             |    }
             |""".stripMargin
      }
    }

    val header =
      s"""package japgolly.scalajs.react.hooks
         |
         |// DO NOT MANUALLY EDIT
         |// DO NOT MANUALLY EDIT
         |//
         |// THIS IS GENERATED BY RUNNING genHooks IN SBT
         |//
         |// DO NOT MANUALLY EDIT
         |// DO NOT MANUALLY EDIT
         |""".stripMargin.trim

    def save(filename: String)(content: String): Unit = {
      println(s"Generating $filename ...")
      val c = content.trim + "\n"
//      println(c)
      IO.write(dir / filename, c)
    }

    save("HookCtx.scala")(
      s"""$header
         |
         |object HookCtx {
         |
         |${hookCtx_apply.result().mkString("\n\n")}
         |
         |  abstract class P0[+P](final val props: P)
         |
         |${hookCtx_Ps.result().mkString("\n")}
         |}
         |""".stripMargin
    )

    save("HookCtxFn.scala")(
      s"""$header
         |
         |// Note: these are never instantiated. They're just here to serve as type lambdas in Scala 2.
         |sealed trait HookCtxFn { type Fn[A] }
         |
         |object HookCtxFn {
         |${hookCtxFns.result().mkString("\n")}
         |}
         |""".stripMargin
    )

    save("StepBoilerplate.scala")(
      s"""$header
         |
         |import HookComponentBuilder._
         |
         |trait DslMultiSteps { self: DslMulti.type =>
         |${dslAtSteps.result().mkString("\n")}
         |}
         |
         |trait StepMultiInstances { self: StepMulti.type =>
         |
         |${stepMultis.result().mkString("\n")}
         |}
         |""".stripMargin
    )

    println()
  }
}