import kotlin.math.cos
import kotlin.math.sin

/** Converted from AffineTransform.java and reduced to only what is needed for this project. */
class AffineTransform {
	var scaleX: Double
	var shearY = 0.0
	var shearX = 0.0
	var scaleY: Double
	var translateX = 0.0
	var translateY = 0.0
	private var state = 0
	private var type = 0

	constructor() {
		scaleY = 1.0
		scaleX = scaleY
	}

	private fun updateState() {
		if (shearX == 0.0 && shearY == 0.0) {
			if (scaleX == 1.0 && scaleY == 1.0) {
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_IDENTITY
					type = TYPE_IDENTITY
				} else {
					state = APPLY_TRANSLATE
					type = TYPE_TRANSLATION
				}
			} else {
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_SCALE
					type = TYPE_UNKNOWN
				} else {
					state = APPLY_SCALE or APPLY_TRANSLATE
					type = TYPE_UNKNOWN
				}
			}
		} else {
			if (scaleX == 0.0 && scaleY == 0.0) {
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_SHEAR
					type = TYPE_UNKNOWN
				} else {
					state = APPLY_SHEAR or APPLY_TRANSLATE
					type = TYPE_UNKNOWN
				}
			} else {
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_SHEAR or APPLY_SCALE
					type = TYPE_UNKNOWN
				} else {
					state = APPLY_SHEAR or APPLY_SCALE or APPLY_TRANSLATE
					type = TYPE_UNKNOWN
				}
			}
		}
	}

	private fun stateError() {
		throw Exception("missing case in transform state switch")
	}

	private fun translate(tx: Double, ty: Double) {
		when (state) {
			APPLY_SHEAR or APPLY_SCALE or APPLY_TRANSLATE -> {
				translateX = tx * scaleX + ty * shearX + translateX
				translateY = tx * shearY + ty * scaleY + translateY
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_SHEAR or APPLY_SCALE
					if (type != TYPE_UNKNOWN) {
						type -= TYPE_TRANSLATION
					}
				}
				return
			}
			APPLY_SHEAR or APPLY_SCALE -> {
				translateX = tx * scaleX + ty * shearX
				translateY = tx * shearY + ty * scaleY
				if (translateX != 0.0 || translateY != 0.0) {
					state = APPLY_SHEAR or APPLY_SCALE or APPLY_TRANSLATE
					type = type or TYPE_TRANSLATION
				}
				return
			}
			APPLY_SHEAR or APPLY_TRANSLATE -> {
				translateX = ty * shearX + translateX
				translateY = tx * shearY + translateY
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_SHEAR
					if (type != TYPE_UNKNOWN) {
						type -= TYPE_TRANSLATION
					}
				}
				return
			}
			APPLY_SHEAR -> {
				translateX = ty * shearX
				translateY = tx * shearY
				if (translateX != 0.0 || translateY != 0.0) {
					state = APPLY_SHEAR or APPLY_TRANSLATE
					type = type or TYPE_TRANSLATION
				}
				return
			}
			APPLY_SCALE or APPLY_TRANSLATE -> {
				translateX = tx * scaleX + translateX
				translateY = ty * scaleY + translateY
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_SCALE
					if (type != TYPE_UNKNOWN) {
						type -= TYPE_TRANSLATION
					}
				}
				return
			}
			APPLY_SCALE -> {
				translateX = tx * scaleX
				translateY = ty * scaleY
				if (translateX != 0.0 || translateY != 0.0) {
					state = APPLY_SCALE or APPLY_TRANSLATE
					type = type or TYPE_TRANSLATION
				}
				return
			}
			APPLY_TRANSLATE -> {
				translateX = tx + translateX
				translateY = ty + translateY
				if (translateX == 0.0 && translateY == 0.0) {
					state = APPLY_IDENTITY
					type = TYPE_IDENTITY
				}
				return
			}
			APPLY_IDENTITY -> {
				translateX = tx
				translateY = ty
				if (tx != 0.0 || ty != 0.0) {
					state = APPLY_TRANSLATE
					type = TYPE_TRANSLATION
				}
				return
			}
			else -> {
				stateError()
				return
			}
		}
	}

	private fun rotate90() {
		var m0 = scaleX
		scaleX = shearX
		shearX = -m0
		m0 = shearY
		shearY = scaleY
		scaleY = -m0
		var state = rot90conversion[state]
		if (state and (APPLY_SHEAR or APPLY_SCALE) == APPLY_SCALE && scaleX == 1.0 && scaleY == 1.0) {
			state -= APPLY_SCALE
		}
		this.state = state
		type = TYPE_UNKNOWN
	}

	private fun rotate180() {
		scaleX = -scaleX
		scaleY = -scaleY
		val state = state
		if (state and APPLY_SHEAR != 0) {
			shearX = -shearX
			shearY = -shearY
		} else {
			if (scaleX == 1.0 && scaleY == 1.0) {
				this.state = state and APPLY_SCALE.inv()
			} else {
				this.state = state or APPLY_SCALE
			}
		}
		type = TYPE_UNKNOWN
	}

	private fun rotate270() {
		var m0 = scaleX
		scaleX = -shearX
		shearX = m0
		m0 = shearY
		shearY = -scaleY
		scaleY = m0
		var state = rot90conversion[state]
		if (state and (APPLY_SHEAR or APPLY_SCALE) == APPLY_SCALE && scaleX == 1.0 && scaleY == 1.0) {
			state -= APPLY_SCALE
		}
		this.state = state
		type = TYPE_UNKNOWN
	}

	private fun rotate(theta: Double) {
		val sin: Double = sin(theta)
		if (sin == 1.0) {
			rotate90()
		} else if (sin == -1.0) {
			rotate270()
		} else {
			val cos: Double = cos(theta)
			if (cos == -1.0) {
				rotate180()
			} else if (cos != 1.0) {
				var m0: Double
				var m1: Double
				m0 = scaleX
				m1 = shearX
				scaleX = cos * m0 + sin * m1
				shearX = -sin * m0 + cos * m1
				m0 = shearY
				m1 = scaleY
				shearY = cos * m0 + sin * m1
				scaleY = -sin * m0 + cos * m1
				updateState()
			}
		}
	}

	fun rotate(theta: Double, anchorx: Double, anchory: Double) {
		translate(anchorx, anchory)
		rotate(theta)
		translate(-anchorx, -anchory)
	}

	fun scale(sx: Double, sy: Double) {
		var state = state
		when (state) {
			APPLY_SHEAR or APPLY_SCALE or APPLY_TRANSLATE, APPLY_SHEAR or APPLY_SCALE -> {
				scaleX *= sx
				scaleY *= sy
				shearX *= sy
				shearY *= sx
				if (shearX == 0.0 && shearY == 0.0) {
					state = state and APPLY_TRANSLATE
					if (scaleX == 1.0 && scaleY == 1.0) {
						type = if (state == APPLY_IDENTITY) TYPE_IDENTITY else TYPE_TRANSLATION
					} else {
						state = state or APPLY_SCALE
						type = TYPE_UNKNOWN
					}
					this.state = state
				}
				return
			}
			APPLY_SHEAR or APPLY_TRANSLATE, APPLY_SHEAR -> {
				shearX *= sy
				shearY *= sx
				if (shearX == 0.0 && shearY == 0.0) {
					state = state and APPLY_TRANSLATE
					if (scaleX == 1.0 && scaleY == 1.0) {
						type = if (state == APPLY_IDENTITY) TYPE_IDENTITY else TYPE_TRANSLATION
					} else {
						state = state or APPLY_SCALE
						type = TYPE_UNKNOWN
					}
					this.state = state
				}
				return
			}
			APPLY_SCALE or APPLY_TRANSLATE, APPLY_SCALE -> {
				scaleX *= sx
				scaleY *= sy
				if (scaleX == 1.0 && scaleY == 1.0) {
					this.state = APPLY_TRANSLATE.let { state = state and it; state }
					type = if (state == APPLY_IDENTITY) TYPE_IDENTITY else TYPE_TRANSLATION
				} else {
					type = TYPE_UNKNOWN
				}
				return
			}
			APPLY_TRANSLATE, APPLY_IDENTITY -> {
				scaleX = sx
				scaleY = sy
				if (sx != 1.0 || sy != 1.0) {
					this.state = state or APPLY_SCALE
					type = TYPE_UNKNOWN
				}
				return
			}
			else -> {
				stateError()
				scaleX *= sx
				scaleY *= sy
				shearX *= sy
				shearY *= sx
				if (shearX == 0.0 && shearY == 0.0) {
					state = state and APPLY_TRANSLATE
					if (scaleX == 1.0 && scaleY == 1.0) {
						type = if (state == APPLY_IDENTITY) TYPE_IDENTITY else TYPE_TRANSLATION
					} else {
						state = state or APPLY_SCALE
						type = TYPE_UNKNOWN
					}
					this.state = state
				}
				return
			}
		}
	}

	fun setToTranslation(tx: Double, ty: Double) {
		scaleX = 1.0
		shearY = 0.0
		shearX = 0.0
		scaleY = 1.0
		translateX = tx
		translateY = ty
		if (tx != 0.0 || ty != 0.0) {
			state = APPLY_TRANSLATE
			type = TYPE_TRANSLATION
		} else {
			state = APPLY_IDENTITY
			type = TYPE_IDENTITY
		}
	}

	companion object {
		private val TYPE_UNKNOWN = -1
		val TYPE_IDENTITY = 0
		val TYPE_TRANSLATION = 1
		val APPLY_IDENTITY = 0
		val APPLY_TRANSLATE = 1
		val APPLY_SCALE = 2
		val APPLY_SHEAR = 4

		private val rot90conversion = intArrayOf(
			APPLY_SHEAR,
			APPLY_SHEAR or APPLY_TRANSLATE,
			APPLY_SHEAR,
			APPLY_SHEAR or APPLY_TRANSLATE,
			APPLY_SCALE,
			APPLY_SCALE or APPLY_TRANSLATE,
			APPLY_SHEAR or APPLY_SCALE,
			APPLY_SHEAR or APPLY_SCALE or APPLY_TRANSLATE
		)
	}
}
