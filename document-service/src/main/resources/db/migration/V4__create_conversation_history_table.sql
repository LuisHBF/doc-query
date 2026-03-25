CREATE TABLE conversation_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    retrieved_chunk_ids UUID[] NOT NULL DEFAULT '{}',
    asked_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversation_history_document_id ON conversation_history(document_id);
CREATE INDEX idx_conversation_history_user_id ON conversation_history(user_id);