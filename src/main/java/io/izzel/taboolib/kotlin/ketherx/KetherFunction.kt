package io.izzel.taboolib.kotlin.ketherx

import io.izzel.kether.common.api.Quest
import java.util.*
import kotlin.collections.HashMap

/**
 * your health {{player health}}, your name {{player name}}
 */
/**
 * 用来解析一些内容的方法
 * 比如你可以获得血量 {{player health}}
 * 或者是获得名称 {{player name}}
 * 你可以可以使用一些比较复杂的方法 {{a == a}} 返回 true {{a == b}} 返回false
 *
 * @constructor Kteher解析方法。
 */
object KetherFunction {

    val regex = Regex("\\{\\{(.*?)}}")

    val scriptMap = HashMap<String, Quest>()
    val functionMap = HashMap<String, Function>()

    /**
     * 解析字符串
     * 一般情况下 cacheFunction 设置为 false  cacheScript 设置为 true
     * 若追求更高性能可都设置为true 但是请注意小心内存泄漏
     *
     * @param input         要判断的文字。
     * @param cacheFunction 是否缓存函数结构。
     * @param cacheScript   是否缓存脚本实例。
     * @param context       上下文判断。
     *
     * @return              解析过的字符串。
     */
    fun parse(
        input: String,
        cacheFunction: Boolean = false,
        cacheScript: Boolean = true,
        context: ScriptContext.() -> Unit = {}
    ): String {
        val function = if (cacheFunction) this.functionMap.computeIfAbsent(input) {
            input.toFunction()
        } else {
            input.toFunction()
        }
        val script = if (cacheScript) this.scriptMap.computeIfAbsent(function.source) {
            ScriptLoader.load(it)
        } else {
            ScriptLoader.load(function.source)
        }
        val vars = ScriptContext.create(script).also(context).run {
            runActions()
            rootFrame().variables()
        }
        return function.element.joinToString("") {
            if (it.isFunction) {
                vars.get<Any>(it.hash).orElse("{{${it.value}}}").toString()
            } else {
                it.value
            }
        }
    }

    /**
     * 把字符串转为函数解析式
     *
     * @param this         要转换的文字。
     *
     * @return              解析过函数式。
     */
    fun String.toFunction(): Function {
        val element = ArrayList<Element>()
        var index = 0
        regex.findAll(this).forEach {
            element.add(Element(substring(index, it.range.first)))
            element.add(Element(it.groupValues[1], true))
            index = it.range.last + 1
        }
        val last = Element(substring(index, length))
        if (last.value.isNotEmpty()) {
            element.add(last)
        }
        return Function(element, element.filter { it.isFunction }.joinToString(" ") {
            "set ${it.hash} to ${it.value}"
        })
    }

    class Element(var value: String, var isFunction: Boolean = false) {

        val hash: String
            get() = value.hashCode().toString()
    }

    class Function(val element: List<Element>, val source: String)
}