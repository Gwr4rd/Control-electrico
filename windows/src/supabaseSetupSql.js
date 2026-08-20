export const SUPABASE_SETUP_SQL = `-- Control Electrico - almacenamiento sincronizado por cuenta
-- Ejecutar una sola vez en Supabase > SQL Editor.
-- Este script se puede compartir: solo crea tabla, permisos y politicas RLS.
-- No contiene claves, contrasenas ni tokens. No compartas la clave service_role.

create table if not exists public.control_electrico_sync (
    user_id uuid primary key references auth.users(id) on delete cascade,
    payload jsonb not null default '{}'::jsonb,
    revision bigint not null default 1 check (revision > 0),
    updated_at timestamptz not null default timezone('utc', now())
);

alter table public.control_electrico_sync enable row level security;

revoke all on table public.control_electrico_sync from anon;
grant select, insert, update, delete on table public.control_electrico_sync to authenticated;

drop policy if exists "control_electrico_select_own" on public.control_electrico_sync;
create policy "control_electrico_select_own"
on public.control_electrico_sync for select to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "control_electrico_insert_own" on public.control_electrico_sync;
create policy "control_electrico_insert_own"
on public.control_electrico_sync for insert to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists "control_electrico_update_own" on public.control_electrico_sync;
create policy "control_electrico_update_own"
on public.control_electrico_sync for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists "control_electrico_delete_own" on public.control_electrico_sync;
create policy "control_electrico_delete_own"
on public.control_electrico_sync for delete to authenticated
using ((select auth.uid()) = user_id);

create or replace function public.control_electrico_set_updated_at()
returns trigger language plpgsql security invoker set search_path = public
as $$
begin
    new.updated_at = timezone('utc', now());
    return new;
end;
$$;

drop trigger if exists control_electrico_updated_at on public.control_electrico_sync;
create trigger control_electrico_updated_at
before update on public.control_electrico_sync
for each row execute function public.control_electrico_set_updated_at();`;
