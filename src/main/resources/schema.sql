CREATE TABLE IF NOT EXISTS public.stocks (
	code varchar NOT NULL,
	name varchar NOT NULL,
	CONSTRAINT stocks_pk PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.bonds (
	code varchar NOT NULL,
	isin_code varchar NULL,
	name varchar NOT NULL,
	type varchar NULL,
	bond_id int4 NULL,
	issue_date timestamp NULL,
	listing_date timestamp NULL,
	mature_date timestamp NULL,
	sharia bool NULL,
	interest_rate numeric NULL,
	interest_type varchar NULL,
	interest_frequency_code varchar NULL,
	interest_frequency varchar NULL,
	CONSTRAINT bonds_pk PRIMARY KEY (code)
);

CREATE TABLE IF NOT EXISTS public.bond_daily (
	id text DEFAULT gen_random_uuid() NOT NULL,
	bond_code varchar NOT NULL,
	bond_id int4 NULL,
	is_transacted bool NULL,
	"date" date NOT NULL,
	date_based timestamp NULL,
	high_price numeric NULL,
	low_price numeric NULL,
	last_price numeric NULL,
	wap numeric NULL,
	total_vol numeric NULL,
	total_val numeric NULL,
	freq numeric NULL,
	one_day_return numeric NULL,
	one_week_return numeric NULL,
	mtd_return numeric NULL,
	one_month_return numeric NULL,
	three_month_return numeric NULL,
	six_month_return numeric NULL,
	ytd_return numeric NULL,
	one_year_return numeric NULL,
	three_year_return numeric NULL,
	five_year_return numeric NULL,
	ten_year_return numeric NULL,
	inception_return numeric NULL,
	ttm numeric NULL,
	ytm numeric NULL,
	current_yield numeric NULL,
	modified_duration numeric NULL,
	outstanding_amount numeric NULL,
	additional_wap numeric NULL,
	CONSTRAINT bond_daily_pk PRIMARY KEY (id),
	CONSTRAINT bond_daily_unique UNIQUE (bond_code, "date")
);

--CREATE INDEX IF NOT EXISTS bond_daily_date_idx ON public.bond_daily USING btree ("date");

CREATE TABLE IF NOT EXISTS public.stock_daily (
	id text DEFAULT gen_random_uuid() NOT NULL,
	code text NOT NULL,
	opening_price int4 NULL,
	closing_price int4 NULL,
	high_price int4 NULL,
	low_price int4 NULL,
	volume int8 NULL,
	market_cap int8 NULL,
	"date" date NOT NULL,
	created_at date NULL,
	CONSTRAINT stock_daily_pkey PRIMARY KEY (id),
	CONSTRAINT stock_daily_unique UNIQUE (code, date)
);
--CREATE INDEX stock_daily_date_idx ON public.stock_daily USING btree (date);

CREATE TABLE IF NOT EXISTS public.funds (
	id int2 NOT NULL,
	"name" varchar NOT NULL,
	"type" numeric NULL,
	active bool NULL,
	sharia bool NULL,
	CONSTRAINT funds_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.fund_daily (
	id varchar DEFAULT gen_random_uuid() NOT NULL,
	fund_id int2 NOT NULL,
	value numeric NULL,
	daily_return numeric NULL,
	"date" date NOT NULL,
	CONSTRAINT fund_daily_pk PRIMARY KEY (id),
	CONSTRAINT fund_daily_unique UNIQUE (fund_id, date)
);
--CREATE INDEX fund_daily_date_idx ON public.fund_daily USING btree (date);

CREATE TABLE IF NOT EXISTS public.fund_aum (
	id text NOT NULL,
	fund_id int2 NOT NULL,
	value numeric NULL,
	"date" date NOT NULL,
	CONSTRAINT fund_nav_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.fund_unit (
	id text NOT NULL,
	fund_id int2 NOT NULL,
	value numeric NULL,
	"date" date NOT NULL,
	CONSTRAINT fund_unit_pk PRIMARY KEY (id)
);
