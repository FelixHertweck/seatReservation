import { useEffect, useRef, useCallback, useState } from "react";

/**
 * Generic WebSocket hook for establishing and managing WebSocket connections
 * Handles automatic reconnection and message parsing
 *
 * @param url - The WebSocket URL to connect to (null to disable)
 * @param enabled - Whether the connection should be active (default: true)
 * @param onMessage - Optional callback when a message is received
 * @param maxReconnectAttempts - Maximum number of reconnection attempts (default: 5)
 * @param reconnectDelay - Delay between reconnection attempts in ms (default: 3000)
 */
export const useWebSocket = (
  url: string | null,
  enabled: boolean = true,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  onMessage?: (data: any) => void,
  maxReconnectAttempts: number = 5,
  reconnectDelay: number = 3000,
  // optional callback called when connecting state changes
  onConnecting?: (connecting: boolean) => void,
) => {
  const [isConnected, setIsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const connectRef = useRef<(() => void) | null>(null);

  const onMessageRef = useRef(onMessage);
  const onConnectingRef = useRef(onConnecting);
  useEffect(() => {
    onConnectingRef.current = onConnecting;
  }, [onConnecting]);
  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  const connect = useCallback(() => {
    if (!url || !enabled) {
      return;
    }

    try {
      if (onConnectingRef.current) {
        try {
          onConnectingRef.current(true);
        } catch (err) {
          console.error(`[WebSocket] Error calling onConnecting:`, err);
        }
      }
      console.log(`[WebSocket] Connecting to ${url}`);

      const ws = new WebSocket(url);

      ws.onopen = () => {
        console.log(`[WebSocket] Connected to ${url}`);
        reconnectAttemptsRef.current = 0;
        setIsConnected(true);
        if (onConnectingRef.current) {
          try {
            onConnectingRef.current(false);
          } catch (err) {
            console.error(`[WebSocket] Error calling onConnecting false:`, err);
          }
        }
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);

          if (onMessageRef.current) {
            onMessageRef.current(data);
          }
        } catch (error) {
          console.error(`[WebSocket] Error:`, error);
          // Try to handle raw data
          if (onMessageRef.current) {
            onMessageRef.current(event.data);
          }
        }
      };

      ws.onerror = (error) => {
        console.error(`[WebSocket] Error:`, error);
      };

      ws.onclose = () => {
        console.log(`[WebSocket] Connection closed`);
        wsRef.current = null;
        setIsConnected(false);

        // Attempt to reconnect
        if (enabled && reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current += 1;
          console.log(
            `[WebSocket] Attempting to reconnect (${reconnectAttemptsRef.current}/${maxReconnectAttempts}) in ${reconnectDelay}ms`,
          );

          reconnectTimeoutRef.current = setTimeout(() => {
            // call via ref to avoid accessing `connect` before declaration in some lint paths
            if (connectRef.current) connectRef.current();
          }, reconnectDelay);
        } else if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
          console.error(
            `[WebSocket] Max reconnection attempts (${maxReconnectAttempts}) reached`,
          );
        }
      };

      wsRef.current = ws;
    } catch (error) {
      console.error(`[WebSocket] Failed to create connection:`, error);
    }
  }, [url, enabled, maxReconnectAttempts, reconnectDelay]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    if (wsRef.current) {
      wsRef.current.onclose = null;
      wsRef.current.close();
      wsRef.current = null;
    }
    setIsConnected(false);
    if (onConnectingRef.current) {
      try {
        onConnectingRef.current(false);
      } catch (err) {
        console.error(
          `[WebSocket] Error calling onConnecting false from disconnect:`,
          err,
        );
      }
    }
    console.log(`[WebSocket] Disconnected`);
  }, []);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const send = useCallback((data: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      try {
        const message = typeof data === "string" ? data : JSON.stringify(data);
        wsRef.current.send(message);
      } catch (error) {
        console.error(`[WebSocket] Failed to send:`, error);
      }
    } else {
      console.warn(
        `[WebSocket] WebSocket not connected. ReadyState: ${wsRef.current?.readyState}`,
      );
    }
  }, []);

  useEffect(() => {
    // store the current connect fn so that setTimeout callbacks can call it
    connectRef.current = connect;
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    send,
    disconnect,
  };
};
