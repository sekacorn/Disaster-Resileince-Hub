import { useId } from 'react';

/**
 * A labelled form control with its error and hint wired to it.
 *
 * Covers the requirements that are easy to miss when a form is assembled by hand:
 *
 * - 1.3.1 / 3.3.2: a real {@code <label htmlFor>}, never a placeholder standing in
 *   for one. Placeholder text vanishes on first keystroke and is not exposed as a
 *   name by every screen reader.
 * - 3.3.1: the error is referenced by {@code aria-describedby}, so it is read when
 *   focus reaches the field rather than only being visible next to it.
 * - 4.1.3: the error container is a live region, so an error appearing after blur is
 *   announced without the user having to go looking for it.
 * - 1.4.1: invalid state is carried by {@code aria-invalid} and by the error text,
 *   not by the red border alone.
 *
 * @param {string} label            visible label text
 * @param {string} [error]          validation message, shown when {@code touched}
 * @param {boolean} [touched]       whether the field has been interacted with
 * @param {string} [hint]           persistent help text
 * @param {boolean} [required]      marks the control required
 * @param {React.ReactNode} children render prop receiving the props to spread onto
 *   the control: {@code id}, {@code aria-describedby}, {@code aria-invalid},
 *   {@code aria-required}
 */
const FormField = ({ label, error, touched, hint, required = false, children }) => {
  // useId keeps ids unique when the same field renders twice on one page, which
  // duplicate hardcoded ids would silently break for assistive technology.
  const fieldId = useId();
  const errorId = `${fieldId}-error`;
  const hintId = `${fieldId}-hint`;

  const showError = Boolean(touched && error);

  const describedBy = [hint ? hintId : null, showError ? errorId : null]
    .filter(Boolean)
    .join(' ') || undefined;

  return (
    <div>
      <label htmlFor={fieldId} className="block text-sm font-medium mb-2">
        {label}
        {required && (
          <>
            <span aria-hidden="true" className="ml-1 text-danger-600">
              *
            </span>
            {/* The asterisk alone is a visual convention; state it in text too. */}
            <span className="sr-only"> (required)</span>
          </>
        )}
      </label>

      {hint && (
        <p id={hintId} className="text-sm text-gray-600 dark:text-gray-400 mb-2">
          {hint}
        </p>
      )}

      {children({
        id: fieldId,
        'aria-describedby': describedBy,
        'aria-invalid': showError ? 'true' : undefined,
        'aria-required': required ? 'true' : undefined,
      })}

      {/*
        Rendered unconditionally so the live region is already in the accessibility
        tree when the message arrives. A region added at the same moment as its text
        is frequently missed by screen readers.
      */}
      <div id={errorId} role="alert" aria-live="polite" className="min-h-[1.25rem]">
        {showError && (
          <span className="text-danger-600 dark:text-danger-400 text-sm mt-1 inline-block">
            {error}
          </span>
        )}
      </div>
    </div>
  );
};

export default FormField;
