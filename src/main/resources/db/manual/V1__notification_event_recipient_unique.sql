DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT constraint_info.conname
        FROM pg_constraint constraint_info
        WHERE constraint_info.conrelid = 'notifications'::regclass
          AND constraint_info.contype = 'u'
          AND (
              SELECT array_agg(attribute_info.attname::text ORDER BY key_column.ordinality)
              FROM unnest(constraint_info.conkey) WITH ORDINALITY AS key_column(attnum, ordinality)
              JOIN pg_attribute attribute_info
                ON attribute_info.attrelid = constraint_info.conrelid
               AND attribute_info.attnum = key_column.attnum
          ) = ARRAY['event_id']::text[]
    LOOP
        EXECUTE format('ALTER TABLE notifications DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint constraint_info
        WHERE constraint_info.conrelid = 'notifications'::regclass
          AND constraint_info.contype = 'u'
          AND (
              SELECT array_agg(attribute_info.attname::text ORDER BY key_column.ordinality)
              FROM unnest(constraint_info.conkey) WITH ORDINALITY AS key_column(attnum, ordinality)
              JOIN pg_attribute attribute_info
                ON attribute_info.attrelid = constraint_info.conrelid
               AND attribute_info.attnum = key_column.attnum
          ) = ARRAY['event_id', 'recipient_user_id']::text[]
    ) THEN
        ALTER TABLE notifications
            ADD CONSTRAINT uk_notification_event_recipient
            UNIQUE (event_id, recipient_user_id);
    END IF;
END $$;
