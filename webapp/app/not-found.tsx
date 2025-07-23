import Link from "next/link";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 text-gray-800">
      <h1 className="text-6xl font-bold text-red-600">404</h1>
      <h2 className="text-2xl font-semibold mt-4 mb-2">Seite nicht gefunden</h2>
      <p className="text-lg text-center mb-8">
        Die angeforderte Seite konnte nicht gefunden werden.
      </p>
      <Link
        href="/"
        className="px-6 py-3 bg-blue-600 text-white rounded-lg shadow-md hover:bg-blue-700 transition duration-300"
      >
        Zur Startseite
      </Link>
    </div>
  );
}
