package com.sobik.model

/**
 * Supported puzzle sizes. Big cubes (4x4+) currently only have learning content;
 * the flags let UI/registries enable scanner/solver per type when they are added.
 */
enum class CubeType(
    val size: Int,
    val label: String,
    val supportsScan: Boolean,
    val supportsSolver: Boolean,
) {
    CUBE_2X2(2, "2x2", supportsScan = true, supportsSolver = true),
    CUBE_3X3(3, "3x3", supportsScan = true, supportsSolver = true),
    CUBE_4X4(4, "4x4", supportsScan = false, supportsSolver = false),
    CUBE_5X5(5, "5x5", supportsScan = false, supportsSolver = false),
    CUBE_6X6(6, "6x6", supportsScan = false, supportsSolver = false),
    CUBE_7X7(7, "7x7", supportsScan = false, supportsSolver = false);

    val stickersPerFace: Int get() = size * size
    val stickerCount: Int get() = 6 * stickersPerFace
    val hasFixedCenters: Boolean get() = size % 2 == 1

    companion object {
        fun ofSize(size: Int): CubeType =
            entries.firstOrNull { it.size == size } ?: throw IllegalArgumentException("Unsupported cube size $size")
    }
}
