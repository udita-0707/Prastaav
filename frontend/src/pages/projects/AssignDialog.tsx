import { AssignReviewerPanel } from '../../components/AssignReviewerPanel';
import { Modal } from '../../components/ui';

export interface AssignTarget {
  id: string;
  title: string;
}

/** Inline "Assign faculty" from a list row. Assigning moves the project from Submitted to Under Review. */
export function AssignDialog({ target, onClose }: { target: AssignTarget | null; onClose: () => void }) {
  return (
    <Modal open={target !== null} onClose={onClose} title="Assign faculty" description={target ? `Choose who reviews “${target.title}”. The project moves to Under Review.` : undefined}>
      {target && <AssignReviewerPanel projectId={target.id} projectTitle={target.title} onAssigned={onClose} />}
    </Modal>
  );
}
