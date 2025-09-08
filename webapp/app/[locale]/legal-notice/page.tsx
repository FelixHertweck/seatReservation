export default async function LegalNoticePage() {
  return (
    <div className="container max-w-4xl py-8">
      <h1 className="text-3xl font-bold mb-8"></h1>

      <div className="prose prose-gray max-w-none space-y-6">
        <section>
          <h2 className="text-2xl font-semibold mb-4">Company Information</h2>
          <div className="bg-muted/50 p-4 rounded-lg">
            <p className="font-medium">[Company Name]</p>
            <p className="text-muted-foreground">[Street Address]</p>
            <p className="text-muted-foreground">[City, State, ZIP Code]</p>
            <p className="text-muted-foreground">[Country]</p>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Contact Information</h2>
          <div className="space-y-2">
            <p>
              <span className="font-medium">Phone:</span>{" "}
              <span className="text-muted-foreground">[Phone Number]</span>
            </p>
            <p>
              <span className="font-medium">Email:</span>{" "}
              <span className="text-muted-foreground">[Email Address]</span>
            </p>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Business Registration</h2>
          <div className="space-y-2">
            <p className="text-muted-foreground">
              Registration details and business license information will be
              listed here.
            </p>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">
            Responsible for Content
          </h2>
          <div className="bg-muted/50 p-4 rounded-lg">
            <p className="font-medium">[Responsible Person Name]</p>
            <p className="text-muted-foreground">[Address]</p>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Disclaimer</h2>
          <h3 className="text-xl font-medium mb-2">Liability for Content</h3>
          <p className="text-muted-foreground leading-relaxed">
            This is a placeholder legal notice. Replace with actual legal
            information that complies with applicable laws and regulations.
          </p>

          <h3 className="text-xl font-medium mb-2 mt-6">Liability for Links</h3>
          <p className="text-muted-foreground leading-relaxed">
            Information about liability for external links and third-party
            content will be provided here.
          </p>
        </section>
      </div>
    </div>
  );
}
