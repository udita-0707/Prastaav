export function ErrorNote({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p className="error-note" role="alert">
      {message}
    </p>
  );
}
