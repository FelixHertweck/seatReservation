export default async function PrivacyPage() {
  return (
    <div className="container max-w-4xl py-8">
      <h1 className="text-3xl font-bold mb-8">Privacy Policy</h1>

      <div className="prose prose-gray max-w-none space-y-6">
        <section>
          <h2 className="text-2xl font-semibold mb-4">
            Data Protection at a Glance
          </h2>
          <p className="text-muted-foreground leading-relaxed">
            This is a placeholder privacy policy page. Replace this content with
            your actual privacy policy that complies with applicable data
            protection laws.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Data Collection</h2>
          <p className="text-muted-foreground leading-relaxed">
            Information about how personal data is collected and processed on
            this website will be detailed here.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Data Usage</h2>
          <p className="text-muted-foreground leading-relaxed">
            Details about how collected data is used, stored, and protected will
            be provided in this section.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Your Rights</h2>
          <p className="text-muted-foreground leading-relaxed">
            Information about user rights regarding their personal data,
            including access, correction, and deletion rights.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold mb-4">Contact</h2>
          <p className="text-muted-foreground leading-relaxed">
            For questions about this privacy policy, please contact us using the
            information provided in the legal notice.
          </p>
        </section>
      </div>
    </div>
  );
}
