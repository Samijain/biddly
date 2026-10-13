import SearchRounded from '@mui/icons-material/SearchRounded'
import Chip from '@mui/material/Chip'
import InputAdornment from '@mui/material/InputAdornment'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'

import { useCategories } from '../hooks'
import type { AuctionFilter, AuctionSort } from '../types'

const sortOptions: { value: AuctionSort; label: string }[] = [
  { value: 'ENDING_SOON', label: 'Ending soon' },
  { value: 'NEWEST', label: 'Newly listed' },
  { value: 'PRICE_LOW', label: 'Price: low to high' },
  { value: 'PRICE_HIGH', label: 'Price: high to low' },
]

interface AuctionFiltersProps {
  value: AuctionFilter
  onChange: (next: AuctionFilter) => void
}

/** A "controlled" component: it owns no state, the parent passes the value and receives changes. */
export function AuctionFilters({ value, onChange }: AuctionFiltersProps) {
  const { data: categories = [] } = useCategories()

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          fullWidth
          placeholder="Search cameras, art, sneakers..."
          value={value.search}
          onChange={(e) => onChange({ ...value, search: e.target.value })}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRounded />
                </InputAdornment>
              ),
            },
            htmlInput: { 'aria-label': 'Search auctions' },
          }}
        />
        <TextField
          select
          label="Sort by"
          value={value.sort}
          onChange={(e) => onChange({ ...value, sort: e.target.value as AuctionSort })}
          sx={{ minWidth: 220 }}
        >
          {sortOptions.map((o) => (
            <MenuItem key={o.value} value={o.value}>
              {o.label}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
        <Chip
          label="All"
          clickable
          color={value.categorySlug === null ? 'primary' : 'default'}
          variant={value.categorySlug === null ? 'filled' : 'outlined'}
          onClick={() => onChange({ ...value, categorySlug: null })}
        />
        {categories.map((c) => {
          const selected = value.categorySlug === c.slug
          return (
            <Chip
              key={c.id}
              label={c.name}
              clickable
              color={selected ? 'primary' : 'default'}
              variant={selected ? 'filled' : 'outlined'}
              onClick={() => onChange({ ...value, categorySlug: selected ? null : c.slug })}
            />
          )
        })}
      </Stack>
    </Stack>
  )
}
