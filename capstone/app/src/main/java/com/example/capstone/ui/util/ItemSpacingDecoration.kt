package com.example.capstone.ui.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 항목 간 간격을 추가하는 아이템 데코레이션
 *
 * @param spacing 항목 간 간격(픽셀 단위)
 * @param applyToFirstItem 첫 번째 항목에도 상단 간격을 적용할지 여부
 * @param applyToLastItem 마지막 항목에도 하단 간격을 적용할지 여부
 */
class ItemSpacingDecoration(
    private val spacing: Int,
    private val applyToFirstItem: Boolean = false,
    private val applyToLastItem: Boolean = true
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        
        // 위치가 유효하지 않으면 무시
        if (position == RecyclerView.NO_POSITION) return
        
        val itemCount = parent.adapter?.itemCount ?: 0
        
        // 첫 번째 항목
        if (position == 0) {
            if (applyToFirstItem) {
                outRect.top = spacing
            }
        }
        
        // 마지막 항목
        if (position == itemCount - 1) {
            if (applyToLastItem) {
                outRect.bottom = spacing
            }
        } else {
            // 마지막이 아닌 모든 항목에 하단 간격 적용
            outRect.bottom = spacing
        }
    }
} 
