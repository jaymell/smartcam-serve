package smartcam

data class Video(
  val start: Float,
  val end: Float,
  val width: Int,
  val height: Int,
  val bucket: String,
  val key: String,
  val region: String
)
