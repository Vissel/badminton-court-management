import React, { useRef } from "react";
import Paper from "@mui/material/Paper";

// Put this class on a dialog's title/header element to make it the drag handle.
export const DIALOG_DRAG_HANDLE = "dialog-drag-handle";

/**
 * PaperComponent for MUI <Dialog>. Makes every dialog using it:
 *  - draggable by its title (any element with the `dialog-drag-handle` class),
 *  - resizable via the browser-native bottom-right grip (CSS resize).
 */
const DraggableResizablePaper = React.forwardRef(function DraggableResizablePaper(
  props,
  ref
) {
  const { sx, onPointerDown, ...other } = props;
  const paperRef = useRef(null);

  const setRefs = (el) => {
    paperRef.current = el;
    if (typeof ref === "function") ref(el);
    else if (ref) ref.current = el;
  };

  const handlePointerDown = (e) => {
    onPointerDown?.(e);
    if (e.button !== 0) return;
    if (!e.target.closest?.(`.${DIALOG_DRAG_HANDLE}`)) return;
    // Keep clicks on interactive elements inside the title working
    if (
      e.target.closest(
        "button, input, textarea, select, a, [contenteditable='true']"
      )
    ) {
      return;
    }

    const paper = paperRef.current;
    if (!paper) return;
    e.preventDefault();

    // Continue from the offset left by previous drags instead of jumping
    // back to the centered position.
    const prev = paper._dragOffset || { x: 0, y: 0 };
    const startX = e.clientX - prev.x;
    const startY = e.clientY - prev.y;

    const onMove = (ev) => {
      const x = ev.clientX - startX;
      const y = ev.clientY - startY;
      paper._dragOffset = { x, y };
      paper.style.transform = `translate(${x}px, ${y}px)`;
    };
    const onUp = () => {
      document.removeEventListener("pointermove", onMove);
      document.removeEventListener("pointerup", onUp);
    };
    document.addEventListener("pointermove", onMove);
    document.addEventListener("pointerup", onUp);
  };

  return (
    <Paper
      {...other}
      ref={setRefs}
      onPointerDown={handlePointerDown}
      sx={{ resize: "both", overflow: "auto", ...sx }}
    />
  );
});

export default DraggableResizablePaper;
