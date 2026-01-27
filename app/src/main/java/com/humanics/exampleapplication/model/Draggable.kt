package com.humanics.exampleapplication.model

/**
 * Draggable 인터페이스
 *
 * iOS의 Draggable 프로토콜과 대응:
 * ```swift
 * protocol Draggable: Identifiable, Codable, Equatable, Transferable {
 *     var id: Int { get }
 * }
 * ```
 *
 * HMDraggableList에서 사용할 아이템은 이 인터페이스를 구현해야 함.
 * id 속성을 통해 아이템을 고유하게 식별하며, 드래그 앤 드롭 시
 * ClipData를 통해 id를 전달함.
 */
interface Draggable {
    val id: Int
}
