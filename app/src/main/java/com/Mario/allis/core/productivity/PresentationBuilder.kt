package com.Mario.allis.core.productivity

data class Slide(
    val title: String,
    val bullets: List<String>,
    val notes: String = ""
)

data class Presentation(
    val title: String,
    val slides: List<Slide>
)

class PresentationBuilder {

    fun build(title: String, sections: Map<String, List<String>>): Presentation {
        val slides = sections.map { (sectionTitle, bullets) ->
            Slide(title = sectionTitle, bullets = bullets)
        }
        return Presentation(title = title, slides = slides)
    }

    fun exportMarkdown(presentation: Presentation): String {
        val builder = StringBuilder()
        builder.append("# ").append(presentation.title).append("\n\n")
        presentation.slides.forEachIndexed { index, slide ->
            builder.append("## ").append(index + 1).append(". ").append(slide.title).append("\n")
            slide.bullets.forEach { bullet ->
                builder.append("- ").append(bullet).append("\n")
            }
            if (slide.notes.isNotBlank()) {
                builder.append("\n> Notas: ").append(slide.notes).append("\n")
            }
            builder.append("\n")
        }
        return builder.toString()
    }
}
