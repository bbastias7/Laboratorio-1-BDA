import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs `fn(signal)` on mount and whenever `deps` change; exposes { data, error, loading, reload }.
 * The previous request is aborted so stale responses never overwrite newer ones.
 */
export function useAsync(fn, deps = []) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const [tick, setTick] = useState(0);
  const fnRef = useRef(fn);
  fnRef.current = fn;

  useEffect(() => {
    const controller = new AbortController();
    setState((s) => ({ ...s, error: null, loading: true }));
    Promise.resolve(fnRef.current(controller.signal))
      .then((data) => { if (!controller.signal.aborted) setState({ data, error: null, loading: false }); })
      .catch((error) => {
        if (controller.signal.aborted || error?.name === 'AbortError') return;
        setState({ data: null, error, loading: false });
      });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { ...state, reload };
}
