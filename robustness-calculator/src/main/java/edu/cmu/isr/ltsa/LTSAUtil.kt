package edu.cmu.isr.ltsa

import lts.*

/**
 * @author Changjian Zhang
 */
class LTSACall {
  init {
    SymbolTable.init()
  }

  fun doCompile(fsp: String, compositeName: String = "DEFAULT"): CompositeState {
    val ltsInput = StringLTSInput(fsp)
    val ltsOutput = StringLTSOutput()
    val compiler = LTSCompiler(ltsInput, ltsOutput, System.getProperty("user.dir"))
    try {
      return compiler.compile(compositeName)
    } catch (e: LTSException) {
      println(e)
      throw Exception("Failed to compile the fsp source string of machine '${compositeName}'.")
    }
  }
}

/**
 *
 */
fun CompositeState.doCompose(): CompositeState {
  val ltsOutput = StringLTSOutput()
  try {
    this.compose(ltsOutput)
    return this
  } catch (e: LTSException) {
    println(e)
    throw Exception("Failed to compose machine '${this.name}'.")
  }
}

/**
 *
 */
fun CompositeState.propertyCheck(): List<String>? {
  val ltsOutput = StringLTSOutput()
  this.analyse(ltsOutput)
  val out = ltsOutput.getText()
  return if (out.contains("""property.*violation""".toRegex())) {
    this.errorTrace.map { (it as String) }
  } else {
    null
  }
}

/**
 *
 */
fun CompositeState.getAllAlphabet(): Set<String> {
  val alphabet = this.machines
      .flatMap { (it as CompactState).alphabet.toList() }
      .fold(mutableSetOf<String>()) { s, a ->
        s.add(a)
        s
      }
  alphabet.remove("tau")
  return alphabet
}

/**
 * The composite state should be composed first.
 */
fun CompositeState.minimise(): CompositeState {
  val ltsOutput = StringLTSOutput()
  this.minimise(ltsOutput)
  return this
}

/**
 * The composite state should be composed first.
 */
fun CompositeState.determinise(): CompositeState {
  val ltsOutput = StringLTSOutput()
  this.determinise(ltsOutput)
  return this
}

/**
 *
 */
fun CompositeState.getCompositeName(): String {
  doCompose()
  return if (this.name != "DEFAULT") {
    this.name
  } else {
    (this.machines[0] as CompactState).name
  }
}

/**
 *
 */
private fun renameConsts(spec: String, prefix: String): String {
  val p = """(const|range)\s+([_\w]+)\s*=""".toRegex()
  var re = spec
  p.findAll(spec).forEach {
    val name = it.groupValues[2]
    re = re.replace(name, "${prefix}_$name")
  }
  return re
}

fun combineSpecs(vararg specs: String): String {
  return specs.joinToString("\n")
//  assert(specs.size <= 26)
//  val a = 'A'
//  return specs.mapIndexed { idx, s -> renameConsts(s, (a + idx).toString()) }.joinToString("\n")
}