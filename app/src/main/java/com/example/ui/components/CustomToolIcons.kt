package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom vector icons drawn with precision paths for tools where Material Icons
 * do not provide an accurate representation (Bubble Level vial, Angle protractor,
 * Metal Detector coil & flux waves, Speaker Cleaner driver + water droplets,
 * Frequency sine oscillator, etc.).
 */
object CustomToolIcons {

    /**
     * Bubble Level: Horizontal spirit level vial with center calibration ticks and floating air bubble.
     */
    val BubbleLevel: ImageVector by lazy {
        ImageVector.Builder(
            name = "CustomBubbleLevel",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer rounded spirit vial
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 7f)
                lineTo(19f, 7f)
                quadTo(22f, 7f, 22f, 10f)
                lineTo(22f, 14f)
                quadTo(22f, 17f, 19f, 17f)
                lineTo(5f, 17f)
                quadTo(2f, 17f, 2f, 14f)
                lineTo(2f, 10f)
                quadTo(2f, 7f, 5f, 7f)
                close()
            }
            // Left and right center vial guide lines
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 7.5f)
                lineTo(9f, 16.5f)
                moveTo(15f, 7.5f)
                lineTo(15f, 16.5f)
            }
            // Center floating bubble circle
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(14.3f, 12f)
                arcTo(2.3f, 2.3f, 0f, true, true, 9.7f, 12f)
                arcTo(2.3f, 2.3f, 0f, true, true, 14.3f, 12f)
                close()
            }
        }.build()
    }

    /**
     * Angle Meter: Two-arm precision protractor with an inner degree measurement arc.
     */
    val AngleMeter: ImageVector by lazy {
        ImageVector.Builder(
            name = "CustomAngleMeter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Base horizontal arm + angled upper arm
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 19f)
                lineTo(4f, 19f)
                lineTo(17.5f, 5.5f)
            }
            // Inner angle measurement arc
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(13f, 19f)
                quadTo(13f, 14f, 10.2f, 12.2f)
            }
            // Degree symbol circle
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(17.7f, 12.5f)
                arcTo(1.5f, 1.5f, 0f, true, true, 14.7f, 12.5f)
                arcTo(1.5f, 1.5f, 0f, true, true, 17.7f, 12.5f)
                close()
            }
        }.build()
    }

    /**
     * Metal Detector: Search coil with handle shaft and radiating electromagnetic field waves.
     */
    val MetalDetector: ImageVector by lazy {
        ImageVector.Builder(
            name = "CustomMetalDetector",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Diagonal detector shaft
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 3f)
                lineTo(10.5f, 14f)
                moveTo(16f, 3f)
                lineTo(20.5f, 5.5f)
            }
            // Search coil ellipse at bottom
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 15.5f)
                quadTo(10.5f, 12.5f, 17f, 15.5f)
                quadTo(10.5f, 18.5f, 4f, 15.5f)
                close()
            }
            // Radiating magnetic flux waves underneath coil
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.5f, 19.2f)
                quadTo(10.5f, 21f, 14.5f, 19.2f)
                moveTo(8.5f, 21.6f)
                quadTo(10.5f, 22.6f, 12.5f, 21.6f)
            }
        }.build()
    }

    /**
     * Speaker Cleaner: Acoustic speaker cone ejecting water droplets with sound pressure wave.
     */
    val SpeakerCleaner: ImageVector by lazy {
        ImageVector.Builder(
            name = "CustomSpeakerCleaner",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Speaker body and cone
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 9.5f)
                lineTo(6.5f, 9.5f)
                lineTo(11.5f, 5f)
                lineTo(11.5f, 19f)
                lineTo(6.5f, 14.5f)
                lineTo(3f, 14.5f)
                close()
            }
            // Acoustic wave arc
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14.5f, 8.5f)
                quadTo(17.2f, 12f, 14.5f, 15.5f)
            }
            // Ejected water droplets
            path(
                fill = SolidColor(Color.White)
            ) {
                // Upper droplet
                moveTo(19.5f, 6.2f)
                quadTo(21.3f, 8.2f, 19.5f, 9.6f)
                quadTo(17.7f, 8.2f, 19.5f, 6.2f)
                close()
                // Lower droplet
                moveTo(20.2f, 13.8f)
                quadTo(22.0f, 15.8f, 20.2f, 17.2f)
                quadTo(18.4f, 15.8f, 20.2f, 13.8f)
                close()
            }
        }.build()
    }

    /**
     * Frequency Generator: Continuous smooth harmonic sine wave with baseline axis ticks.
     */
    val FrequencyWave: ImageVector by lazy {
        ImageVector.Builder(
            name = "CustomFrequencyWave",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Continuous sine wave curve
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 12f)
                curveTo(4.2f, 3.5f, 6.8f, 3.5f, 9f, 12f)
                curveTo(11.2f, 20.5f, 13.8f, 20.5f, 16f, 12f)
                curveTo(17.8f, 5.5f, 20.2f, 5.5f, 22f, 12f)
            }
            // Subtle center reference dots
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(5.5f, 12f)
                arcTo(1f, 1f, 0f, true, true, 3.5f, 12f)
                arcTo(1f, 1f, 0f, true, true, 5.5f, 12f)
                close()
                moveTo(19.5f, 12f)
                arcTo(1f, 1f, 0f, true, true, 17.5f, 12f)
                arcTo(1f, 1f, 0f, true, true, 19.5f, 12f)
                close()
            }
        }.build()
    }
}
