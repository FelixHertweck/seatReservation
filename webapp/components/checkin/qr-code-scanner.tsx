"use client";

import { useState, useEffect, useRef } from "react";
import jsQR from "jsqr";
import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from "@/components/ui/card";
import { toast } from "@/hooks/use-toast";
import { Camera, CameraOff, Loader2, QrCode } from "lucide-react";

interface QrCodeScannerProps {
  onScan: (data: ScannedData) => void;
  isScanning: boolean;
  setIsScanning: (isScanning: boolean) => void;
  scannedData: ScannedData | null;
  setScannedData: (data: ScannedData | null) => void;
}

export interface ScannedData {
  userId: string;
  eventId: string;
  checkInTokens: string[];
}

export function QrCodeScanner({
  onScan,
  isScanning,
  setIsScanning,
  scannedData,
  setScannedData,
}: QrCodeScannerProps) {
  const t = useT();
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [isCameraLoading, setIsCameraLoading] = useState(false);

  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const scanIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Start camera
  const startCamera = async () => {
    setIsCameraLoading(true);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
      });
      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        videoRef.current.onloadedmetadata = () => {
          setIsCameraLoading(false);
          setIsCameraActive(true);
          setIsScanning(true);
        };
      }
    } catch (error) {
      console.error("Error accessing camera:", error);
      toast({
        title: t("checkin.qrScanner.cameraError"),
        variant: "destructive",
      });
      setIsCameraLoading(false);
    }
  };

  // Stop camera
  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }

    setIsCameraActive(false);
    stopScanning();
    setScannedData(null);
  };

  // Stop scanning for QR codes
  const stopScanning = () => {
    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current);
      scanIntervalRef.current = null;
    }
    setIsScanning(false);
  };

  // Start scanning for QR codes
  const startScanning = () => {
    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current);
    }
    setIsScanning(true);
    scanIntervalRef.current = setInterval(() => {
      scanQRCode();
    }, 500);
  };

  // Scan QR code from video
  const scanQRCode = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;

    if (!video || !canvas || video.readyState !== video.HAVE_ENOUGH_DATA) {
      return;
    }

    const context = canvas.getContext("2d", { willReadFrequently: true });
    if (!context) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
    const code = jsQR(imageData.data, imageData.width, imageData.height);

    if (code) {
      handleQRCodeScanned(code.data);
    }
  };

  // Handle scanned QR code
  const handleQRCodeScanned = (data: string) => {
    try {
      // Expected format: userId;eventId;token1,token2,...
      const parts = data.split(";");
      if (parts.length !== 3) {
        throw new Error("Invalid format");
      }

      const userId = parts[0];
      const eventId = parts[1];

      // Validate that userId and eventId are valid numbers (will throw if invalid)
      try {
        BigInt(userId);
        BigInt(eventId);
      } catch (e) {
        console.log("Error parsing QR code data:", e);
        throw new Error("Invalid user ID or event ID");
      }

      const checkInTokens = parts[2]
        .split(",")
        .filter((token) => token.trim() !== "");

      onScan({ userId, eventId, checkInTokens });

      // Stop scanning after successful scan
      stopScanning();
    } catch (error) {
      console.log("Error parsing QR code data:", error);
    }
  };

  // Handle isScanning changes
  useEffect(() => {
    if (isCameraActive && isScanning) {
      startScanning();
    } else {
      stopScanning();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isScanning, isCameraActive]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopCamera();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Card>
      <CardHeader>
        <CardDescription className="flex items-center gap-2">
          <QrCode className="h-5 w-5" />
          {t("checkin.qrScanner.description")}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="relative h-[300px] md:h-[400px] bg-black rounded-lg overflow-hidden">
          <video
            ref={videoRef}
            className="w-full h-full object-cover"
            playsInline
          />
          <canvas ref={canvasRef} className="hidden" />

          {!isCameraActive && !isCameraLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-muted">
              <CameraOff className="h-16 w-16 text-muted-foreground" />
            </div>
          )}

          {isCameraLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/50 text-white text-lg font-semibold">
              <Loader2 className="h-8 w-8 animate-spin mr-2" />
              {t("checkin.qrScanner.loadingCamera")}
            </div>
          )}

          {isCameraActive && !isScanning && scannedData && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/50 text-white text-lg font-semibold">
              {t("checkin.qrScanner.reservationsOpen")}
            </div>
          )}
        </div>

        <div className="flex gap-2">
          {!isCameraActive ? (
            <Button
              onClick={startCamera}
              className="flex-1"
              disabled={isCameraLoading}
            >
              {isCameraLoading ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <Camera className="mr-2 h-4 w-4" />
              )}
              {t("checkin.qrScanner.startCamera")}
            </Button>
          ) : (
            <Button
              onClick={stopCamera}
              variant="destructive"
              className="flex-1"
            >
              <CameraOff className="mr-2 h-4 w-4" />
              {t("checkin.qrScanner.stopCamera")}
            </Button>
          )}
        </div>

        {scannedData && (
          <Card className="bg-muted">
            <CardContent className="pt-4">
              <div className="text-sm space-y-1">
                <p>
                  <strong>User ID:</strong> {scannedData.userId}
                </p>
                <p>
                  <strong>Event ID:</strong> {scannedData.eventId}
                </p>
                <p>
                  <strong>Tokens:</strong>{" "}
                  {scannedData.checkInTokens.join(", ")}
                </p>
              </div>
            </CardContent>
          </Card>
        )}
      </CardContent>
    </Card>
  );
}
