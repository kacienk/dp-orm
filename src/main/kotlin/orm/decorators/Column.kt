package orm.decorators

import javax.swing.text.StyledEditorKit.BoldAction

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val nullable: Boolean = true
)
