import { LocationEditor } from "@/components/management/location-editor/location-editor";

export async function generateStaticParams() {
  return [{ locationId: "placeholder" }];
}

export default async function LocationEditorPage({
  params,
}: {
  params: Promise<{ locationId: string }>;
}) {
  const { locationId } = await params;

  return (
    <div className="container mx-auto px-4 sm:px-6">
      <LocationEditor locationId={locationId} />
    </div>
  );
}
