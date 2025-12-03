# Brand and interface guidelines

Ledgerline should read as a banking product built by a team, not as a showcase. The interface uses
a neutral product palette, a single accent colour and conventional banking patterns: sidebar, page
header, summary cards, tables and forms.

## Palette

| Token | Value | Use |
|---|---|---|
| `paper` | `#F7F8FA` | application background |
| `surface` | `#FFFFFF` | cards, sidebar and header |
| `ink` | `#101828` | primary text |
| `slate` | `#475467` | body text |
| `muted` | `#667085` | secondary text and placeholders |
| `line` | `#E4E7EC` | borders and dividers |
| `primary` | `#1D4ED8` | buttons, links, active navigation, focus rings |
| `success` | `#047857` | incoming amounts and positive states |
| `danger` | `#B91C1C` | errors and destructive actions |

There is no second accent colour. Status colours appear only in badges and amounts.

## Type

Inter for everything, with tabular figures for money. Weights: 400 body, 500 labels and navigation,
600 headings and amounts. Section headings are 24px on desktop and 20px on mobile; the dashboard
total is the only larger figure.

## Layout

- Sidebar fixed at 260px with the mark, navigation items with icons and the session block at the
  bottom. The active item uses a light blue background and primary text.
- Header with the section name and the user avatar.
- Content column capped at 1152px, 32px padding on desktop and 16px on mobile.
- Cards: white surface, 1px border, 12px radius, 24px padding, almost invisible shadow.
- Forms: 40px inputs with a visible border and a 2px focus ring in the accent colour.
- Tables: grey header row, 1px dividers, hover on the row, no zebra stripes.
- Badges: pill shaped, soft background, 12px text.

## Mark

The mark is a rounded square in the accent colour with an L cut out of it. It works at 24px and
reads as a product icon rather than an illustration.

- `client/components/wordmark.tsx`: `Mark` (icon) and `Wordmark` (icon + name).
- `client/app/icon.svg`: favicon derived from the mark.

## Voice

Sentence case, short sentences, no exclamation marks. Buttons describe the action ("Transfer",
"Open account"); errors say what happened and what to do next. Amounts always carry a currency,
are right-aligned in tables and use tabular figures.

## Writing about the product

- "Open an account", "Transfer", "Statement": sentence case in prose and in buttons.
- Status values appear in badges in sentence case: `Verified`, `Completed`, `Pending`.
