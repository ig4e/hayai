package yokai.util.widget

fun scaleFlingVelocity(velocity: Int, itemCount: Int): Int {
    val factor = 1f + (itemCount / 500f).coerceIn(0f, 4f)
    return (velocity * factor).toInt()
}
