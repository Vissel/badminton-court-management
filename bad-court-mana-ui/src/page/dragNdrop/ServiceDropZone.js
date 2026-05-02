import { useDrop } from "react-dnd";
import { ItemTypes } from "../ItemTypes";
const ServiceDropZone = ({ player, children, onDrop ,onDropService}) => {
  const [, drop] = useDrop({
    accept: ItemTypes.SERVICE,
    drop: (item) => onDrop(item.name),
  });
  const [, dropService] = useDrop({
    accept: ItemTypes.SERVICE,
    drop: (item) => player && onDropService(player, item.name),
  });

  return (
    <div className="player-drop-zone"
    ref={(el) => {
        // drop(el);
        dropService(el);
      }}
      >
      {children}
    </div>
  );
};
// Not use yet
export default ServiceDropZone;