UPDATE available_player
SET pay_type = 'CASH'
WHERE pay_type = 'PAY';

UPDATE available_player
SET pay_type = NULL,
    is_canceled = TRUE
WHERE pay_type = 'CANCEL';
